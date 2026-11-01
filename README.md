# marketplace-multi-category

Multi-category marketplace for the Research Industry Projects course.
M1 is verified. M2 begins with registration, session login/logout and authenticated
listing creation. React, listing editing/deletion and filtering remain next.

## Run everything in Docker

Requirements: Docker Desktop with Linux containers and Compose v2. No host Java,
Maven or Python is needed. From the repository root:

```powershell
docker compose up -d --build --wait
```

- [Swagger UI](http://localhost:8081/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8081/v3/api-docs)
- [Health](http://localhost:8081/actuator/health)
- PostgreSQL: `localhost:15432`, database/user `marketplace`, local password `marketplace_local`.

Backend and database communicate through `postgres:5432` internally. PostgreSQL
keeps its data in a named volume. Flyway migrates automatically at startup. The
backend runs as a non-root user and has a database-aware health check.

```powershell
docker compose ps
docker compose logs --tail 100 backend
docker compose exec postgres psql -U marketplace -d marketplace -c "\dt"
docker compose down
```

Stopping/recreating containers preserves data. Rebuild after code changes.
Optionally copy `.env.example` to `.env` to override `BACKEND_PORT`, `POSTGRES_PORT`
and `DB_PASSWORD`. A changed variable does not change an existing database user's password.

## Tests and repeatable demonstration

```powershell
docker compose --profile tools run --build --rm verify
docker compose --profile tools run --rm demo
```

`verify` performs a clean Maven build and PostgreSQL integration tests inside Docker.
It uses the Docker socket for disposable Testcontainers instances. Reports and
generated files appear in ignored `backend/target/`. The Maven cache uses a named
volume. `host.docker.internal` connects the verifier to mapped test ports.

`demo` checks Swagger, health, registration/login, listing creation and retrieval,
validation errors and logout over real HTTP. **It leaves a demo account and listing
in the development database.** Each run uses a unique email.

The application image build uses its own temporary PostgreSQL inside the build stage
for Flyway/jOOQ generation, without a Docker socket or external database. It packages
with tests skipped; run `verify` separately. Only the resulting JAR is copied into
the JRE runtime, which contains no Maven or PostgreSQL and has no Docker socket.

## Swagger authentication flow

Public category/listing GETs need no login. For creating listings:

1. GET `/api/v1/auth/csrf` and copy the returned token.
2. POST `/api/v1/auth/register` using that token in `X-CSRF-TOKEN`, your email,
   displayName and a password of 12+ characters, at most 72 UTF-8 bytes.
3. POST `/api/v1/auth/login` using the same token and your credentials.
4. GET `/api/v1/auth/csrf` again: login rotates the session and invalidates the old token.
5. POST `/api/v1/listings` with the renewed token and the supplied request example.
6. GET `/api/v1/auth/me` shows the account. POST `/api/v1/auth/logout` with the current token ends the session.

The browser manages the HttpOnly session cookie. Registration does not log in
automatically. Legacy M1 seed users have no password and cannot log in.
New listing sellers come from the authenticated account, never from client input.
See [authentication](docs/authentication.md) for the full contract.

## API and optional IDE workflow

The five category/listing operations remain under `/api/v1/categories` and
`/api/v1/listings`. Swagger describes all operations, constraints and errors.
FIXED_PRICE requires a positive price with at most two decimal places. AUCTION
stores metadata only; bidding belongs to M3. Errors include `code` and field details.

Native Java debugging remains available with JDK 21 and Maven 3.9+: start the database
with `docker compose up -d postgres`, then run `mvn spring-boot:run` from `backend/`.
Use `SERVER_PORT` if 8080 is occupied. Native datasource defaults to localhost:15432.
Run `mvn generate-sources` with Docker running and reload `backend/pom.xml` in IntelliJ
if generated types are unresolved. Generated files belong only in
`backend/target/generated-sources/jooq/`, package `com.marketplace.jooq`.

The backend accepts `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` for future external
database hosting. Cloud SQL connectivity and deployment are future work. Local HTTP
session cookies use `secure=false`; set `SESSION_COOKIE_SECURE=true` for HTTPS.
Sessions are currently in memory and end on backend restart; shared/persistent
session storage is needed before deploying multiple instances.

## Documentation

- [Architecture](docs/architecture.md)
- [Database design](docs/database-design.md)
- [Authentication](docs/authentication.md)
- [API errors](docs/api-errors.md)
- [Testing and demo](docs/testing.md)
- [M1 verification report](docs/m1-verification.md)
- [Research proposal](docs/research-proposal.md): planned experiments; no trained model or results yet.
- [Instructions and milestones](AGENTS.md)
