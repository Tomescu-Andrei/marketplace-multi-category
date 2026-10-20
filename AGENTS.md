# AGENTS.md — Multi-Category Marketplace

## 1. Project Overview

**Project name:** Multi-Category Marketplace  
**Repository:** `marketplace-multi-category`  
**Local path:** `E:\MarketPlace\marketplace-multi-category`

The project is a multi-category online marketplace supporting:

- Fixed-price product listings.
- Real-time auctions and bidding.
- Dynamic product attributes depending on category.
- AI-based second-hand price estimation.
- Integration with external product data APIs.
- Deployment on Google Cloud Platform.

The project is developed for the **Research Industry Projects** university course. Development is organized into five milestones, each requiring a demonstrable result.

**Important:** Prioritize incremental development. Do not implement the entire application during the first milestone.

---

## 2. Technology Stack

### Frontend — Future milestone

- React
- JavaScript (NOT TypeScript)
- Vite
- Tailwind CSS
- Google Cloud Run deployment

**Frontend development is explicitly postponed during M1.**

### Backend

- Java 21
- Spring Boot
- Maven
- Spring Web
- Spring Validation
- Spring JDBC / jOOQ integration
- **jOOQ for database access**
- **Flyway for database migrations**
- PostgreSQL
- Springdoc OpenAPI / Swagger UI
- JUnit 5
- Testcontainers
- Docker

**Do not use Spring Data JPA, Hibernate, or `JpaRepository`.**

### AI / Machine Learning

- Python for dataset processing, training and experiments.
- scikit-learn and other appropriate ML libraries.
- Export trained models to a Java-compatible format, preferably ONNX where supported.
- Execute inference directly inside Spring Boot using a compatible Java runtime.

**Do not introduce a Python FastAPI microservice.**

The AI research code is maintained separately from the production backend. Model compatibility and preprocessing equivalence must be verified before integration.

### Google Cloud Platform

Planned production architecture:

- React frontend → Cloud Run
- Spring Boot backend → Cloud Run
- PostgreSQL → Cloud SQL
- Product images → Cloud Storage
- Container images → Artifact Registry
- Secrets → Secret Manager

During M1, development is local using Docker Compose.

---

## 3. Backend Architecture

Use a traditional **layered Spring Boot architecture**, not package-by-feature.

Required packages:

- `controller` — REST endpoints
- `service` — business logic and transactions
- `repository` — SQL queries through jOOQ
- `dto` — API request and response objects
- `config` — application configuration
- `exception` — exception handling

Introduce `model` and `mapper` packages only when necessary.

Do not create redundant model classes that simply duplicate generated jOOQ POJOs.

### Dependency flow

`Controller → Service → Repository (jOOQ) → PostgreSQL`

Controllers must not execute SQL.

Repositories must not contain HTTP concerns.

Business rules and transaction boundaries belong in the service layer.

Use constructor-based dependency injection.

Prefer Java records for simple DTOs.

---

## 4. Backend Directory Structure

```text
marketplace-multi-category/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .dockerignore
│   │
│   └── src/
│       ├── main/
│       │   ├── java/com/marketplace/
│       │   │   ├── MarketplaceApplication.java
│       │   │   │
│       │   │   ├── controller/
│       │   │   │   ├── CategoryController.java
│       │   │   │   └── ListingController.java
│       │   │   │
│       │   │   ├── service/
│       │   │   │   ├── CategoryService.java
│       │   │   │   └── ListingService.java
│       │   │   │
│       │   │   ├── repository/
│       │   │   │   ├── CategoryRepository.java
│       │   │   │   └── ListingRepository.java
│       │   │   │
│       │   │   ├── dto/
│       │   │   │   ├── CategoryResponse.java
│       │   │   │   ├── CreateListingRequest.java
│       │   │   │   └── ListingResponse.java
│       │   │   │
│       │   │   ├── config/
│       │   │   └── exception/
│       │   │       └── GlobalExceptionHandler.java
│       │   │
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       └── db/migration/
│       │           ├── V1__create_categories.sql
│       │           ├── V2__create_users.sql
│       │           └── V3__create_listings.sql
│       │
│       └── test/
│
├── research/
│   ├── datasets/
│   ├── notebooks/
│   └── experiments/
│
├── docs/
├── docker-compose.yml
├── .gitignore
└── README.md
```

The exact Flyway migrations may be reorganized if necessary to satisfy foreign-key dependencies.

**Do not create empty placeholder classes for future modules.**

---

## 5. jOOQ Requirements

jOOQ replaces JPA/Hibernate as the database access mechanism.

### Generated source location

Generate jOOQ code under:

`backend/target/generated-sources/jooq/`

Use the generated Java package:

`com.marketplace.jooq`

Typical generated classes:

```text
target/generated-sources/jooq/
└── com/marketplace/jooq/
    ├── Tables.java
    └── tables/
        ├── Categories.java
        ├── Listings.java
        └── records/
            ├── CategoriesRecord.java
            └── ListingsRecord.java
```

### Rules

1. Never edit generated jOOQ classes.
2. Never commit `target/` to Git.
3. Use `DSLContext` inside repository classes.
4. Use Flyway migrations as the source of truth for database structure.
5. Generate jOOQ classes from the migrated schema.
6. Make the Maven generation workflow reproducible on a clean environment.
7. Ensure generated sources are included in Java compilation.
8. Do not expose generated jOOQ records directly as REST responses.
9. Use explicit database transactions for multi-step operations.

A temporary PostgreSQL database or suitable reproducible generation strategy may be used during jOOQ code generation.

Do not require manually maintained production database schemas to generate code.

---

## 6. M1 — Backend Foundation

**Period:** 21 October 2026 – 3 November 2026

### Objective

Build a working backend skeleton that demonstrates:

- Spring Boot application startup.
- PostgreSQL integration.
- Flyway migrations.
- jOOQ code generation.
- REST API functionality.
- Layered backend architecture.
- Swagger API documentation.
- Local Docker environment.

The backend must be demonstrable without a frontend.

### M1 scope

Implement:

1. Maven Spring Boot project.
2. PostgreSQL configuration.
3. Docker Compose for local infrastructure.
4. Flyway migrations.
5. jOOQ Maven code generation.
6. Categories and category attributes.
7. Basic listings.
8. REST controllers, services and repositories.
9. DTO validation and error handling.
10. Swagger UI.
11. Basic automated tests.
12. Architecture and research documentation.

### Out of scope for M1

Do NOT implement:

- React frontend.
- Full JWT authentication.
- Real-time auction bidding.
- WebSocket communication.
- Payments.
- Messaging.
- Notifications.
- AI inference.
- Google Cloud production deployment.

Future functionality should influence database design, but should not be implemented prematurely.

---

## 7. Initial Database Design

Design the database to accommodate multiple categories and listing types.

Recommended initial entities:

### categories

- `id`
- `name`
- `slug`
- `parent_id` (nullable)

### category_attributes

- `id`
- `category_id`
- `name`
- `code`
- `data_type`
- `required`

### users

- `id`
- `email`
- `display_name`
- `created_at`

For M1, a predefined test user is sufficient. Full authentication will be implemented later.

### listings

- `id`
- `seller_id`
- `category_id`
- `title`
- `description`
- `listing_type` (`FIXED_PRICE`, `AUCTION`)
- `price` (nullable when appropriate)
- `currency`
- `status`
- `created_at`
- `updated_at`

### listing_attributes

Store category-specific attribute values, using an approach justified in the database design.

Possible approaches:

- Relational attribute-value table.
- PostgreSQL JSONB for flexible attributes.

Choose one initial approach and document the trade-offs. Do not overengineer.

### Future tables

Plan for, but do not necessarily implement during M1:

- `auctions`
- `bids`
- `listing_images`
- `favorites`
- `messages`
- `notifications`
- `valuation_results`

Ensure money values use appropriate precision, not floating-point types.

---

## 8. M1 REST API

Implement the following endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/categories` | List categories |
| GET | `/api/v1/categories/{id}` | Category details and attributes |
| GET | `/api/v1/listings` | List listings |
| GET | `/api/v1/listings/{id}` | Listing details |
| POST | `/api/v1/listings` | Create a basic listing |

Use the predefined test seller for M1 rather than implementing full authentication.

Requests must be validated.

Return appropriate HTTP status codes and consistent error responses.

Swagger UI must allow manual testing of the API.

---

## 9. Testing Requirements

At minimum:

- Test category retrieval.
- Test listing creation.
- Test retrieving a newly created listing.
- Test validation of invalid requests.
- Verify Flyway migrations run on a clean database.
- Verify jOOQ code generation succeeds.

Prefer integration tests against PostgreSQL using Testcontainers for repository behavior.

Avoid relying exclusively on mocked repository tests.

---

## 10. Milestone Plan

### M1 — Backend Foundation

21.10.2026 – 03.11.2026

- Spring Boot skeleton.
- PostgreSQL, Flyway and jOOQ.
- Categories and initial listings.
- Swagger.
- Docker Compose.
- Research proposal.

### M2 — Marketplace

04.11.2026 – 17.11.2026

- React frontend.
- User authentication.
- Listing management.
- Category filtering.
- Listing details.

### M3 — Auctions

18.11.2026 – 08.12.2026

- Auction lifecycle.
- Bidding logic.
- Spring WebSocket.
- Concurrent bid validation.
- Bid history and winner determination.

### M4 — AI & Research

09.12.2026 – 22.12.2026

- Dataset preparation.
- Model experiments in Python.
- MAE, RMSE and R² evaluation.
- Java-compatible model export.
- Inference integrated into Spring Boot.

### M5 — Cloud Deployment & Final Evaluation

23.12.2026 – 19.01.2027

- React and Spring Boot deployment to Cloud Run.
- PostgreSQL in Cloud SQL.
- Cloud Storage integration.
- Testing, research evaluation and documentation.
- Final end-to-end demonstration.

Where practical, test an early Cloud Run deployment before M5.

---

## 11. M1 Acceptance Criteria

M1 is considered complete when:

- [ ] Backend starts successfully.
- [ ] PostgreSQL is accessible.
- [ ] Flyway applies database migrations.
- [ ] jOOQ generated sources compile.
- [ ] Category endpoints return real database data.
- [ ] A listing can be created using the REST API.
- [ ] Created listings can be retrieved.
- [ ] Invalid requests return meaningful errors.
- [ ] Swagger UI is accessible.
- [ ] Docker Compose startup is documented.
- [ ] Basic automated tests pass.
- [ ] README explains how to run the application.
- [ ] Initial architecture and research documentation exist.

### M1 Demo Scenario

1. Start the local environment.
2. Show the PostgreSQL database and tables.
3. Explain the Controller → Service → Repository → PostgreSQL flow.
4. Open Swagger UI.
5. Fetch categories.
6. Create a listing.
7. Retrieve the created listing.
8. Show successful tests.
9. Explain how the architecture will support auctions and AI later.

---

## 12. Instructions for the Implementing Agent

Before making any changes:

1. Inspect the existing repository and its current files.
2. Identify already implemented features and configurations.
3. Do not overwrite or delete existing work unnecessarily.
4. Follow the chosen technology stack and package organization.
5. Implement M1 incrementally, prioritizing working code over placeholders.
6. Avoid TypeScript, JPA/Hibernate and a separate AI microservice.
7. Keep jOOQ generated classes outside manually maintained source code.
8. Maintain database structure through Flyway.
9. Add tests for implemented behavior.
10. Document important technical decisions.
11. Do not introduce unnecessary infrastructure such as Kubernetes or Kafka.
12. Clearly report what is implemented, tested and still pending.

**Primary task:** Deliver a minimal, clean, demonstrable Spring Boot backend skeleton for M1, using PostgreSQL, Flyway and jOOQ, with a classic layered architecture and no frontend.

**Do not proceed into M2 functionality unless explicitly requested.**
