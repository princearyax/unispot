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
  * Secured via `@PreAuthorize("hasRole('ADMIN')")`. For example, only campus administrators can create or modify `Place` entities, while authenticated `USER` roles have permissions to post ratings and reviews.

---

## 🗄️ Database Entity-Relationship Diagram (ERD)

```text
+-----------------------+       1:N       +-----------------------+
|         USER          | <-------------> |        REVIEW         |
+-----------------------+                 +-----------------------+
| PK id                 |                 | PK id                 |
|    username           |                 |    comment            |
|    email              |                 |    rating             |
|    password           |                 | FK user_id            |
|    role (ADMIN/USER)  |                 | FK place_id           |
+-----------------------+                 +-----------------------+
            |                                         |
            | 1:N                                     | 1:N
            v                                         v
+-----------------------+                 +-----------------------+
|         PLACE         | <-------------> |      PLACE_IMAGE      |
+-----------------------+                 +-----------------------+
| PK id                 |                 | PK id                 |
|    name               |                 |    secure_url         |
|    description        |                 |    public_id          |
|    coordinates        |                 | FK place_id           |
|    version (@Version) |                 +-----------------------+
+-----------------------+
