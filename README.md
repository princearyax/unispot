# UniSpot — Hyperlocal Campus Discovery & Review Backend

[![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Spring Security](https://img.shields.io/badge/Spring_Security-JWT_%2B_RBAC-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Cloudinary](https://img.shields.io/badge/Cloudinary-Media_Storage-3448C5?style=flat-square&logo=cloudinary&logoColor=white)](https://cloudinary.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![OpenAPI 3](https://img.shields.io/badge/OpenAPI_3.0-Swagger_UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)](https://swagger.io/)

**UniSpot** is a high-concurrency RESTful backend engineered to index and review granular university campus locations (study rooms, cafes, food stalls, libraries, sports facilities) overlooked by global mapping providers.

Built with **Java 21 (Virtual Threads)** and **Spring Boot 4.1.1**, UniSpot focuses on production backend engineering tradeoffs: $O(\log N)$ keyset pagination, optimistic concurrency control (`@Version`) with isolated transaction retries, single-pass JWT verification with rotating `HttpOnly` refresh cookies, and non-transactional cloud media streaming that prevents JDBC connection pool starvation.

---

## Key Engineering Highlights

### 1. Optimistic Concurrency Control (`@Version`) & Self-Healing Rating Aggregation
* **Lost-Update Prevention:** `Place` entities track a JPA `@Version` column. Concurrent `PUT /api/v1/places/{id}` updates must supply the known version; stale writes fail fast with `409 Conflict` (`ObjectOptimisticLockingFailureException`) without holding pessimistic `SELECT ... FOR UPDATE` row locks.
* **Event-Driven `AFTER_COMMIT` Rating Recalculation:** When a student creates or deletes a `Review`, `ReviewService` publishes a `ReviewChangedEvent`. A `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` listener triggers `PlaceService.recalculateRating()` *after* the review transaction commits.
* **Proxy-Safe Retry Isolation (`Propagation.REQUIRES_NEW`):** Each rating recalculation attempt delegates to a dedicated `PlaceRatingUpdater` component annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)`. This avoids Spring AOP self-invocation traps, provides a fresh `EntityManager` per retry, and computes a self-healing `COALESCE(AVG(r.rating), 0.0), COUNT(r)` aggregate directly in PostgreSQL.

### 2. $O(\log N)$ Keyset (Cursor) Pagination & Constructor DTO Projections
* **Why Keyset Over Offset:** Standard `OFFSET N LIMIT M` pagination forces PostgreSQL to scan and discard $N$ rows ($O(N)$ I/O cost). UniSpot implements cursor pagination (`WHERE p.category = :category AND p.id < :lastId ORDER BY p.id DESC`) backed by a composite B-Tree index `idx_place_category_id (category, id DESC)`.
* **Zero-`COUNT(*)` `hasNext` Detection:** The service requests `size + 1` rows from PostgreSQL. If `size + 1` rows return, `hasNext` is `true`, the list is sliced to `size`, and `nextCursor` is set to the last item's `id`—eliminating expensive `SELECT COUNT(*)` queries.
* **Cache-Friendly Record Projections:** Queries project directly into immutable Java 21 `record` DTOs (`PlaceSummaryDto`) rather than full JPA entities, reducing memory footprint and supporting direct Jackson serialization.

### 3. Java 21 Virtual Threads & Non-Transactional Cloudinary Streaming
* **Virtual Threads (`spring.threads.virtual.enabled=true`):** Blocking network I/O automatically unmounts virtual threads from carrier OS threads.
* **Preventing HikariCP Connection Pool Starvation:** Wrapping slow third-party HTTP calls (like Cloudinary uploads) inside `@Transactional` holds a leased JDBC connection for seconds, exhausting HikariCP under concurrent load. `PlaceService.uploadImage()` uses a **3-phase non-transactional orchestration pattern**:
  1. **Phase 1 (~2 ms read transaction):** Verify place existence, ownership (`AuthorizationHelper`), and max quota (`<= 5` images), then immediately release the DB connection.
  2. **Phase 2 (Network I/O outside transaction):** Validate file size (`<= 5MB`), MIME type, and **binary Magic Bytes** (`JPEG`, `PNG`, `WebP` headers), then stream to Cloudinary.
  3. **Phase 3 (~3 ms write transaction + Compensating Rollback):** Persist the returned `secure_url` into `@ElementCollection Set<String> imageUrls`. If the database transaction fails, a `catch` block executes a compensating `destroy` call to delete the orphaned Cloudinary asset.

### 4. Stateless JWT Access Tokens + Rotating `HttpOnly` Refresh Cookies
* **Single-Pass Claims Verification:** `JwtService` caches the HMAC-SHA256 `SecretKey` at startup (`@PostConstruct`) and parses/validates signature, expiration, `sub` (`userId`), and `role` claims in a single pass per request.
* **Refresh Token Rotation:** Long-lived (7-day) opaque UUID refresh tokens are persisted in PostgreSQL (`refresh_tokens` table) and transmitted exclusively via `HttpOnly`, `SameSite`, path-restricted (`/api/v1/auth`) cookies to mitigate XSS token theft. Calling `/api/v1/auth/refresh` invalidates the used token and rotates a fresh token and cookie.
* **Filter-Chain Error Consistency:** Custom `RestAuthenticationEntryPoint` (`401`) and `RestAccessDeniedHandler` (`403`) serialize errors inside the Spring Security filter chain using the exact same `ErrorResponse` JSON contract as `GlobalExceptionHandler`.

### 5. Hibernate & PostgreSQL Performance Patterns
* **Sequence ID Batching:** Entities use `GenerationType.SEQUENCE` with explicit `@SequenceGenerator` allocation sizes, avoiding `IDENTITY` insert-time flush bottlenecks.
* **Unidirectional `@ManyToOne` & Proxy References:** `User` and `Place` intentionally omit `@OneToMany List<Review>` collections to prevent accidental lazy-loading heap exhaustion. Child `Review` insertions use `getReferenceById()` Hibernate proxies after lightweight `existsById()` checks to avoid unnecessary `SELECT *` queries.
* **Bulk JPQL Deletes:** Deleting a `Place` executes a single `@Modifying @Query("DELETE FROM Review r WHERE r.place.id = :placeId")` bulk delete rather than loading thousands of `Review` entities into JVM memory for cascade deletion.

---

## Project Architecture (Pragmatic Package-by-Feature)

The codebase is organized by **domain feature** (`user`, `place`, `review`) rather than technical layer, with cross-cutting infrastructure isolated in `core`:

```text
com.prince.unispot
├── UnispotApplication.java
├── core/                                   # Cross-cutting concerns
│   ├── application/storage/                # ImageStorageService (Port)
│   ├── infrastructure/storage/             # CloudinaryImageStorageService (Adapter + Magic-Byte check)
│   ├── config/                             # ApplicationConfig, JpaConfig, CloudinaryConfig, OpenApiConfig, AdminBootstrapRunner
│   ├── domain/                             # BaseTimeEntity, AuditableEntity, ReviewChangedEvent
│   ├── exception/                          # GlobalExceptionHandler, ErrorResponse, custom exceptions
│   └── security/                           # SecurityConfig, JwtService, JwtAuthenticationFilter, AuthorizationHelper
├── user/                                   # Feature: Authentication, Refresh Tokens, & Admin RBAC
│   ├── domain/model/                       # User, RefreshToken, Role
│   ├── application/service/                # AuthService, RefreshTokenService, UserAdminService
│   ├── infrastructure/persistence/         # UserRepository, RefreshTokenRepository
│   └── presentation/                       # AuthController, AdminController, Auth DTO records
├── place/                                  # Feature: Campus Places, Cursor Pagination, & Media
│   ├── domain/model/                       # Place (@Version, @ElementCollection Set<String>), Category
│   ├── application/service/                # PlaceService, PlaceRatingUpdater, PlaceImageTransactionHelper
│   ├── infrastructure/persistence/         # PlaceRepository (@EntityGraph, Cursor JPQL)
│   └── presentation/                       # PlaceController, CursorPageResponse, Place DTO records
└── review/                                 # Feature: Ratings & Reviews
    ├── domain/model/                       # Review (Composite unique constraint: place_id + user_id)
    ├── application/service/                # ReviewService
    ├── infrastructure/persistence/         # ReviewRepository, RatingAggregate
    └── presentation/                       # ReviewController, Review DTO records
```

---

## Database Schema & Indexing Strategy

* **`users`**: Indexed on `idx_user_email (email UNIQUE)`.
* **`refresh_tokens`**: Indexed on `idx_refresh_token (token UNIQUE)`. Holds `user_id` FK (`LAZY`) and `expiry_date`.
* **`places`**: Extends `AuditableEntity` (`created_by`, `created_at`, `updated_at`). Indexed on `idx_place_category_id (category, id DESC)` to serve keyset pagination via index-range scans. Tracks `@Version version`, `average_rating`, and `review_count`.
* **`place_images`**: `@ElementCollection` join table (`place_id`, `image_url`) backed by a `Set<String>` to ensure targeted delta `INSERT`/`DELETE` statements.
* **`reviews`**: Indexed on `idx_review_place_id (place_id)` with composite unique constraint `uk_review_place_user (place_id, user_id)` enforcing one review per student per place at the database level.

---

## API Reference

Interactive **Swagger UI** is available at **`http://localhost:8080/swagger-ui.html`** (raw OpenAPI 3.0 JSON spec at **`http://localhost:8080/v3/api-docs`**).

### 1. Authentication (`/api/v1/auth`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register a student account; returns `accessToken` and sets `HttpOnly` refresh cookie. | Public |
| `POST` | `/api/v1/auth/login` | Authenticate credentials; returns `accessToken` and sets `HttpOnly` refresh cookie. | Public |
| `POST` | `/api/v1/auth/refresh` | Rotate `HttpOnly` refresh token cookie and issue a new short-lived `accessToken`. | Cookie (`refreshToken`) |
| `POST` | `/api/v1/auth/logout` | Delete refresh tokens for the user in DB and clear the `HttpOnly` cookie (`Max-Age=0`). | Cookie (`refreshToken`) |

### 2. Campus Places (`/api/v1/places`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/places/cursor` | Fetch places by `category`, `lastId` (optional cursor), and `size` (`CursorPageResponse`). | Public |
| `GET` | `/api/v1/places/{id}` | Fetch full details of a place including eager-loaded `imageUrls` and `@Version`. | Public |
| `POST` | `/api/v1/places` | Create a new campus place (`createdBy` populated via Spring Data JPA Auditing). | `USER`, `ADMIN` |
| `PUT` | `/api/v1/places/{id}` | Update a place (requires matching `version` field for optimistic concurrency check). | Creator, `ADMIN` |
| `POST` | `/api/v1/places/{id}/images` | Upload a place image (`multipart/form-data`, JPEG/PNG/WebP `<= 5MB`, max 5 per place). | Creator, `ADMIN` |
| `DELETE` | `/api/v1/places/{id}/images` | Remove an image URL from the place and delete the asset from Cloudinary. | Creator, `ADMIN` |
| `DELETE` | `/api/v1/places/{id}` | Delete a place, bulk-delete its reviews in DB, and purge its Cloudinary images. | Creator, `ADMIN` |

### 3. Reviews (`/api/v1/reviews`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/reviews/place/{placeId}` | Get paginated reviews for a place (`Slice<ReviewResponse>`, supports `page`, `size`, `sort`). | Public |
| `POST` | `/api/v1/reviews/place/{placeId}` | Submit a rating (1–5) and comment; triggers `AFTER_COMMIT` place rating recalculation. | `USER`, `ADMIN` |
| `DELETE` | `/api/v1/reviews/{id}` | Delete a review and recalculate the parent place's rating aggregate. | Creator, `ADMIN` |

### 4. Admin (`/api/v1/admin/users`)
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `PATCH` | `/api/v1/admin/users/{id}/role` | Promote or demote a user's role (`USER` / `ADMIN`). | `ADMIN` |

---

## Local Setup & Developer Guide

### Prerequisites
* **Docker & Docker Compose** (for containerized execution or running PostgreSQL locally)
* **Java 21 LTS** (only required if running the Spring Boot app directly on your host machine outside Docker)
* **Git**

---

### Environment Variables Reference

All sensitive credentials and environment-specific settings are externalized via environment variables with sensible local defaults in `src/main/resources/application.yml`:

| Variable | Default (Local Dev) | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP server port for the Spring Boot application. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/unispot` | JDBC URL (automatically overridden to `postgres:5432` inside Docker Compose). |
| `DB_USERNAME` | `postgres` | PostgreSQL database username. |
| `DB_PASSWORD` | `password` | PostgreSQL database password. |
| `JPA_DDL_AUTO` | `update` | Hibernate schema mode (`update`, `create-drop`, or `validate`). |
| `JWT_SECRET` | *(32-byte Base64 dev key)* | **Must be Base64-encoded and >= 256 bits (32 bytes).** Generate via: `openssl rand -base64 32` |
| `CLOUDINARY_URL` | *(empty — placeholder mode)* | Format: `cloudinary://<api_key>:<api_secret>@<cloud_name>` from your Cloudinary Dashboard. |
| `ADMIN_EMAIL` | `admin@unispot.dev` | Seeding email used by `AdminBootstrapRunner` on startup (skipped if blank). |
| `ADMIN_PASSWORD` | `AdminPass123!` | Seeding password for the initial `ADMIN` user (hashed with BCrypt). |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated list of allowed frontend origins. |
| `COOKIE_SAME_SITE` | `Lax` | `Lax` for local HTTP testing; set to `None` with `COOKIE_SECURE=true` for cross-site HTTPS. |
| `COOKIE_SECURE` | `false` | Set to `true` in HTTPS production environments. |
| `SWAGGER_ENABLED` | `true` | Set to `false` in production to disable `/swagger-ui.html` and `/v3/api-docs`. |

---

### Option 1: Full Docker Compose Setup (Fastest — Zero Local Java/Maven Needed)

This workflow builds the multi-stage `Dockerfile` (Maven + JDK 21 builder stage -> minimal Alpine JRE 21 non-root runtime stage) and starts both PostgreSQL 16 and the UniSpot API on an isolated bridge network.

#### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/unispot.git
cd unispot
```

#### 2. Configure `.env` (Optional)
Copy the example environment file. It works out of the box for all auth, place, and review endpoints; add your `CLOUDINARY_URL` if you want to test live cloud image uploads:
```bash
cp .env.example .env
```

#### 3. Build and start the containers
```bash
docker compose up --build -d
```
* Docker Compose waits for the `unispot-db` PostgreSQL container to pass its `pg_isready` healthcheck before starting `unispot-api`.
* On first boot, `AdminBootstrapRunner` automatically creates the initial `ADMIN` account (`admin@unispot.dev` / `AdminPass123!`).

#### 4. Verify container health and stream logs
```bash
docker compose ps
docker compose logs -f unispot-api
```

#### 5. Stopping and cleaning up
```bash
# Stop containers while preserving PostgreSQL data volume
docker compose down

# Stop containers AND wipe the PostgreSQL volume for a clean database reset
docker compose down -v
```

---

### Option 2: Hybrid Local Development (PostgreSQL in Docker + Spring Boot in IDE/Maven)

Use this workflow when actively writing code in IntelliJ IDEA or VS Code so you get instant compilation and debugger attachment without rebuilding Docker images.

#### 1. Start only the PostgreSQL 16 database container
```bash
docker compose up -d postgres
```
*(Or via standalone Docker CLI if not using Compose:)*
```bash
docker run --name unispot-db \
  -e POSTGRES_DB=unispot \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:16-alpine
```

#### 2. Ensure `application.yml` is present on the classpath
If `src/main/resources/application.yml` is not yet created locally, copy the template:
```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

#### 3. Export optional environment variables (or set them in your IDE Run Configuration)
```bash
export ADMIN_EMAIL="admin@unispot.dev"
export ADMIN_PASSWORD="AdminPass123!"
export CLOUDINARY_URL="cloudinary://your_key:your_secret@your_cloud_name"
```

#### 4. Run the application via the Maven Wrapper
```bash
./mvnw spring-boot:run
```
The API will start on **`http://localhost:8080`**.

---

### End-to-End Verification & Testing Guide

#### 1. Interactive Testing via Swagger UI
1. Open **`http://localhost:8080/swagger-ui.html`** in your browser.
2. Expand **`POST /api/v1/auth/login`**, click **Try it out**, and log in using the bootstrapped Admin account (or register a new user via `/api/v1/auth/register`):
   ```json
   {
     "email": "admin@unispot.dev",
     "password": "AdminPass123!"
   }
   ```
3. Copy the `accessToken` string from the `200 OK` response body. *(Note: Your browser also automatically receives and stores the `HttpOnly` `refreshToken` cookie).*
4. Scroll to the top of Swagger UI, click the green **Authorize** button, paste your `accessToken` into the **Value** field, and click **Authorize**.
5. You can now test all protected endpoints directly from Swagger UI:
   * Create a campus place via **`POST /api/v1/places`** (`category`: `CAFE`, `LIBRARY`, `PARK`, `FOOD_STALL`, `STUDY_ROOM`, or `SPORTS_FACILITY`).
   * Upload a photo (`JPEG`, `PNG`, or `WebP` `<= 5MB`) via **`POST /api/v1/places/{id}/images`**.
   * Post a rating (`1` to `5`) via **`POST /api/v1/reviews/place/{placeId}`**, then call **`GET /api/v1/places/{id}`** to see the updated `averageRating`, `reviewCount`, and incremented `@Version` number.
   * Test keyset pagination via **`GET /api/v1/places/cursor?category=CAFE&size=5`**.

#### 2. Running Automated Unit & Concurrency Retry Tests
The test suite uses **JUnit 5** and **Mockito** to verify:
* Optimistic lock conflict retries and failure thresholds (`PlaceServiceRatingRetryTest`).
* Compensating Cloudinary asset deletion when database transactions fail (`PlaceImageUploadCompensationTest`).
* Idempotent admin account seeding (`AdminBootstrapRunnerTest`).

Run the full test suite with:
```bash
./mvnw clean test
```