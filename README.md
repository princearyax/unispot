# unispot
Community Platform for University Ecosystems

> A high-performance, enterprise-grade hyperlocal navigation and discovery backend engineered specifically for specialized campus environments. UniSpot bridges the gap between static university maps and dynamic community interaction by offering real-time place reviews, secure media handling, and robust access controls.

---

## 🛠️ Tech Stack

*   [![Java](https://img.shields.io/badge/Java-21%20LTS-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
*   [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3%20%2B-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
*   [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
*   [![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-red?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
*   [![Cloudinary](https://img.shields.io/badge/Cloudinary-Media%20SDK-orange?style=flat-square&logo=cloudinary)](https://cloudinary.com/)
*   [![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)

---

## 🏗️ Architecture & Engineering Depth

UniSpot is built from the ground up for maximum throughput, low latency, and future-proof extensibility:

1. **N-Tier Clean Architecture:**
   * **Controllers:** Handle HTTP semantics, routing, and JSON serialization. Business logic is strictly decoupled.
   * **Services:** Encapsulate core domain business rules and external integrations (e.g., Cloudinary SDK).
   * **Repositories:** Manage data persistence and mapping to PostgreSQL using Spring Data JPA.

2. **Java 21 Virtual Threads (Project Loom):**
   * Configured via a single line (`spring.threads.virtual.enabled=true`), the JVM maps millions of lightweight virtual threads to a few OS threads. When blocking I/O operations occur—such as waiting for Cloudinary responses over the network—the JVM unmounts the virtual thread, keeping underlying OS resources free to handle other incoming concurrent student requests.

3. **Optimistic Concurrency Control (`@Version`):**
   * Multi-user concurrent review submissions and rating updates on `Place` entities utilize JPA `@Version` annotations. This prevents race conditions and dirty writes during high-concurrency campus events without locking database rows.

4. **Database Optimization & Projections:**
   * Utilizes Spring Data JPA interface-based projections for GET endpoints, ensuring only exact required database columns are fetched to minimize memory overhead and network bandwidth.

---

## 🔒 Security, Authentication & RBAC

* **JWT Access & Refresh Token Flow:**
  * **Access Tokens:** Short-lived (15 minutes), transmitted via the `Authorization: Bearer <token>` header.
  * **Refresh Tokens:** Long-lived (7 days), securely persisted in the database and delivered via an HTTP-only, secure cookie.
* **Custom Filter Chain:** A custom `OncePerRequestFilter` intercepts requests to validate JWT signatures and populate the SecurityContext.
* **Role-Based Access Control (RBAC):**
  * Secured via `@PreAuthorize("hasRole('ADMIN')")`. For example, admin can delete any user or place.


Using domain Driven Design (DDD)
grouping by feature

Isolation of Domain: Notice how the domain/ folder is at the top of each feature. It contains pure Java. It has no dependencies on Spring MVC (the presentation folder) or the database (the infrastructure folder).

com.prince.unispot
├── UniSpotApplication.java
│
├── core/                               # Cross-cutting concerns (Global)
│   ├── config/                         # SecurityConfig, JpaAuditingConfig
│   ├── domain/                         # AuditableEntity (Shared superclasses)
│   ├── exception/                      # GlobalExceptionHandler
│   └── security/                       # JwtFilter, SecurityUtils
│
├── user/                               # FEATURE: User Identity & Roles
│   ├── domain/
│   │   └── model/                      # User, Role (Enum)
│   ├── application/
│   │   └── service/                    # AuthService, UserService
│   ├── infrastructure/
│   │   └── persistence/                # UserRepository
│   └── presentation/
│       ├── controller/                 # AuthController
│       └── dto/                        # LoginRequest, RegisterRequest
│
├── place/                              # FEATURE: Campus Places
│   ├── domain/
│   │   ├── model/                      # Place, Category (Enum)
│   │   └── exception/                  # PlaceNotFoundException
│   ├── application/
│   │   └── service/                    # PlaceService
│   ├── infrastructure/
│   │   └── persistence/                # PlaceRepository
│   └── presentation/
│       ├── controller/                 # PlaceController
│       └── dto/                        # PlaceResponse (Projections)
│
└── review/                             # FEATURE: Ratings & Reviews
    ├── domain/
    │   └── model/                      # Review
    ├── application/
    │   └── service/                    # ReviewService
    ├── infrastructure/
    │   └── persistence/                # ReviewRepository
    └── presentation/
        ├── controller/                 # ReviewController
        └── dto/                        # ReviewRequest


# 📍 UniSpot: Hyperlocal Campus Navigation API

UniSpot is an enterprise-grade backend service designed to map and index granular, hyperlocal university campus locations (cafes, study rooms, hidden spots) often overlooked by global mapping providers. 

Built with Java 21 and Spring Boot, it features an optimized N-Tier Clean Architecture designed to handle high-concurrency student traffic via Virtual Threads (Project Loom) and Optimistic Locking.

## 🚀 Tech Stack & Dependencies

The project relies on modern Java ecosystem standards defined in the `pom.xml`:

*   **Core:** Java 21, Spring Boot 3.x
*   **Web & Concurrency:** Spring Web (with `spring.threads.virtual.enabled=true`)
*   **Database & ORM:** PostgreSQL, Spring Data JPA, Hibernate 6
*   **Security:** Spring Security, JWT (JSON Web Tokens)
*   **Media Storage:** Cloudinary Java SDK
*   **Productivity:** Lombok

## 🏗 System Architecture

The codebase adheres strictly to **Package-by-Feature** and **Clean Architecture (Domain-Driven Design)** principles:
*   **Domain Isolation:** Entities (`Place`, `User`, `Review`) and Enums are decoupled from web and infrastructure layers.
*   **Optimistic Concurrency Control:** Prevents "Lost Update" anomalies on Place ratings using JPA `@Version`.
*   **ID Batching:** Utilizes `GenerationType.SEQUENCE` with `allocationSize` to enable Hibernate JDBC Write-Behind batch inserts.
*   **Security:** Stateless JWT authentication utilizing short-lived Access Tokens and HTTP-only Refresh Tokens. Role-Based Access Control (RBAC) enforces strict entity ownership (users can only delete their own reviews).

## 🗄️ Database Schema & Relationships

*   **`users`**: Aggregate Root. Extends `BaseTimeEntity`.
*   **`places`**: Aggregate Root. Extends `AuditableEntity`. Tracks the creator via `created_by` for RBAC. Uses an `@ElementCollection` (Set) for `place_images` to prevent JPA delete anomalies.
*   **`reviews`**: Associative Entity. Holds `@ManyToOne` foreign keys to both `users` and `places`. 

*(Note: Bidirectional mappings are strictly managed. Aggregates like `User` do not hold `OneToMany` collections of reviews to prevent JVM memory exhaustion during lazy loading).*

## 🔌 API Endpoints Specification

### 1. Authentication (`/api/v1/auth`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/register` | Register a new student account. | Public |
| `POST` | `/login` | Authenticate and return JWT Access Token. | Public |
| `POST` | `/refresh` | Exchange a valid Refresh Cookie for a new Access Token. | Public |

### 2. Places (`/api/v1/places`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Get paginated places (uses `PlaceSummaryProjection`). | Public |
| `GET` | `/{id}` | Get detailed view of a specific place including images. | Public |
| `POST` | `/` | Create a new place on the campus map. | `USER`, `ADMIN` |
| `DELETE`| `/{id}` | Delete a place (only by Creator or Admin). | Creator, `ADMIN` |
| `POST` | `/{id}/images` | Upload multipart image to Cloudinary & link to Place. | Creator, `ADMIN` |

### 3. Reviews (`/api/v1/reviews`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/place/{placeId}` | Get paginated reviews for a specific place. | Public |
| `POST` | `/place/{placeId}` | Add a review/rating to a place. | `USER` |
| `DELETE`| `/{id}` | Delete a review (only by Creator or Admin). | Creator, `ADMIN` |

## 🛠 Local Setup (Quickstart)

1.  **Start PostgreSQL** via Docker:
    ```bash
    docker run --name unispot-db -e POSTGRES_PASSWORD=password -d -p 5432:5432 postgres
    ```
2.  **Environment Variables**: Configure the following in your IDE or `.bashrc`:
    *   `JWT_SECRET`
    *   `CLOUDINARY_URL`
3.  **Run Application**:
    ```bash
    ./mvnw spring-boot:run
    ```


    ### 🏛️ Clean Architecture & DDD Nomenclature

This project abandons the traditional "Package-by-Layer" monolith in favor of "Package-by-Feature" driven by Domain-Driven Design (DDD) principles. Each bounded context (e.g., `Place`, `Review`) is isolated into four distinct layers:

*   **`domain/` (The Core):** Contains the enterprise business logic (Entities, Enums, Interfaces). It represents the purest form of the business rules. It has **zero dependencies** on external frameworks (no Spring imports, no database specific annotations beyond standard JPA).
*   **`application/` (The Use Cases):** Coordinates the domain objects. It contains Services that implement business workflows (e.g., `PlaceService`). It dictates *what* needs to be done, but delegates the *how* to the infrastructure layer.
*   **`infrastructure/` (The Adapters):** Contains implementations for external tools. This is where Spring Data JPA Repositories live, translating application queries into PostgreSQL dialects, or where Cloudinary SDK logic resides. It adapts the core domain to the outside world.
*   **`presentation/` (The Delivery Mechanism):** Contains the REST Controllers and DTOs/Projections. It is strictly responsible for deserializing HTTP requests into Java objects and routing them to the Application layer.