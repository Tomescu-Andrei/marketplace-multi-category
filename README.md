# marketplace-multi-category

Multi-category marketplace developed for the Research Industry Projects course.

## Current status

Java 21, Spring Boot, Maven, Spring Web, Validation, Flyway, jOOQ and PostgreSQL.
The database runs locally in Docker; the backend runs on the host machine.

The category/listing APIs, validation, consistent errors and Swagger UI are implemented.
M1 architecture and research proposal are documented. No frontend is implemented.

## Run locally

Requirements: JDK 21, Maven 3.9 or newer, and Docker Desktop running with Linux containers.

From the repository root:

```powershell
docker compose up -d --wait
cd backend
mvn spring-boot:run
```

The server listens on `http://localhost:8080`. API endpoints are under `/api/v1`;
a 404 response at `/` is expected. Stop the application with Ctrl+C.

If another project uses port 8080, set `$env:SERVER_PORT = "8081"` before
starting the backend, then use `http://localhost:8081`.

PostgreSQL is available at `localhost:15432`, database `marketplace`, username
`marketplace`, password `marketplace_local` (local development only).
The host port is 15432 to avoid conflicts with other local PostgreSQL instances;
inside the container PostgreSQL uses port 5432.
Flyway automatically creates the schema on first application startup and applies
new migrations on subsequent starts. The default `local` profile also creates an
M1 test seller. When selecting a different profile, configure a seller explicitly;
authentication and production deployment remain out of scope.

From the repository root, inspect or stop the database:

```powershell
docker compose ps
docker compose exec postgres psql -U marketplace -d marketplace -c "SELECT current_database();"
docker compose down
```

Data persists in a named Docker volume when the container is stopped or recreated.

## Database configuration

The backend reads `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` from its environment.
Defaults match the local Docker container. For example, in PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:15432/marketplace"
$env:DB_USERNAME = "marketplace"
$env:DB_PASSWORD = "marketplace_local"
```

Compose also reads `DB_PASSWORD` from the shell environment. Set it before
starting both Compose and the backend when using a different local password.
PostgreSQL initialization settings apply only when its data volume is empty;
changing the variable does not change an existing database user's password.

The production database is planned for Google Cloud SQL. Its connection URL and
credentials will be supplied through environment variables and Secret Manager.
Cloud SQL connectivity and deployment will be configured in a later milestone.

## Verify and package

From `backend/`:

```powershell
mvn clean verify
java -jar target/marketplace-backend-0.0.1-SNAPSHOT.jar
```

Docker must be running for compilation and tests. During Maven's `generate-sources`
phase, a temporary PostgreSQL container is migrated with Flyway and jOOQ generates
Java classes in `target/generated-sources/jooq`, package `com.marketplace.jooq`.
Maven adds this folder to compilation automatically. Generate alone with
`mvn generate-sources`; never edit generated files or commit `target/`.

API integration tests use another isolated PostgreSQL container. They verify
migrations, category retrieval, listing creation and retrieval, pagination,
invalid requests, error contracts and Swagger. Builds and tests do not change the
development database. See [testing and demo](docs/testing.md) for coverage and troubleshooting.

In IntelliJ, import `backend/pom.xml`, select JDK 21, run `mvn generate-sources`
with Docker running, then reload the Maven project if generated types are unresolved.

## REST API

| Method | Path | Result |
|---|---|---|
| GET | `/api/v1/categories` | Seeded categories |
| GET | `/api/v1/categories/{id}` | Category and attribute definitions |
| GET | `/api/v1/listings?limit=20&offset=0` | Listings, newest first |
| GET | `/api/v1/listings/{id}` | Listing details |
| POST | `/api/v1/listings` | Create a listing; returns 201 and a Location header |

Listing pagination accepts a limit from 1 to 100 and a nonnegative offset.
Missing resources return 404. Invalid input returns 400 using Problem Detail JSON.
An unknown category during creation returns 404. Errors include a stable `code`
and an `errors` array of field/message pairs; see [API errors](docs/api-errors.md).

Example in PowerShell (change the port if using `SERVER_PORT`):

```powershell
$baseUrl = "http://localhost:8080"
Invoke-RestMethod "$baseUrl/api/v1/categories"
Invoke-RestMethod "$baseUrl/api/v1/categories/1"
$body = @{
    categoryId = 1
    title = "Used laptop"
    description = "Working laptop"
    listingType = "FIXED_PRICE"
    price = 1250.50
    currency = "RON"
    attributes = @{ brand = "Lenovo"; year = 2022; original_packaging = $false }
} | ConvertTo-Json
$listing = Invoke-RestMethod "$baseUrl/api/v1/listings" -Method Post -ContentType "application/json" -Body $body
Invoke-RestMethod "$baseUrl/api/v1/listings/$($listing.id)"
```

The seller is supplied by the local server configuration, not by the request.
`FIXED_PRICE` requires a positive price, with at most two decimal places.
`AUCTION` stores listing metadata only and allows a null price; bidding and auction
lifecycle are deferred to M3. Both types require a supported uppercase ISO currency.
Category attribute codes, required values and JSON types are validated by the service.

## Swagger UI

With the backend running, open [Swagger UI](http://localhost:8080/swagger-ui.html).
Use **Try it out** on each operation; POST listings includes a working example.
Adjust the URL if using a different `SERVER_PORT`. No authentication is required
for the M1 local seller.

OpenAPI JSON: [API definition](http://localhost:8080/v3/api-docs).
YAML is also available at `/v3/api-docs.yaml`.

## Documentation

- [Architecture](docs/architecture.md): layers, request flow and future cloud architecture.
- [Database design](docs/database-design.md): schema decisions and JSONB trade-offs.
- [API errors](docs/api-errors.md): validation rules and error contract.
- [Testing and demo](docs/testing.md): verification, manual demonstration and troubleshooting.
- [Research proposal](docs/research-proposal.md): dataset requirements, experiments,
  MAE/RMSE/R² evaluation and planned Java model integration. No experiments have run yet.

See [AGENTS.md](AGENTS.md) for the architecture and milestone requirements.
