# M1 verification report

Date: 1 November 2026.

## Clean-source Docker check

A local clone of commit 2b7a2f4 was created in an ignored temporary directory. The
pending Docker changes were overlaid because they had not been committed yet.
The clone began without target/, generated classes, packaged JARs or an application
.env file. A separate Compose project used ports 18081/25432 and a fresh database
volume, preserving the development database.

The following passed using Docker alone, before M2 changes:

- Multi-stage image build, including temporary PostgreSQL, Flyway and jOOQ generation.
- Healthy PostgreSQL and backend startup from the resulting image.
- Clean verification in the Maven container: **55 tests, zero failures/errors/skips**.
- Demo from the actual OpenAPI example: Swagger assets, health, categories, details,
  201 listing creation, retrieval, list, 400 price/attribute errors and 404 for an absent ID.

The clone used its own Maven cache and database. Only the verifier accessed the
Docker socket to create disposable Testcontainers instances. Build/runtime images
did not use the socket. The runtime contained only the JRE, application and health tool.

## Interactive Swagger check

On the Docker-hosted application at port 8081:

- GET categories returned Electronics and Books with HTTP 200.
- POST listings with Swagger's supplied example returned 201 and a Location header.
- GET the created listing returned persisted values with 200.
- GET listing ID 0 returned 400, INVALID_PARAMETER and an id field error.

The interactive check created a sample in the development database. The command-line
demo also created a sample in the separate verification database.

## M1 acceptance

Startup, database connection, clean Flyway migrations, jOOQ compilation, real
category data, listing creation/retrieval, meaningful errors, Swagger and automated
tests all passed. Startup/demo instructions, architecture, database decisions and
the research proposal are documented. Research experiments remain future work.

M2 starts after this checkpoint with authentication. Its current suite extends the
55-test M1 snapshot. Run `docker compose --profile tools run --build --rm verify`
for current results. The legacy M1 seller is preserved; M2 uses the authenticated
account for new listings. See [authentication](authentication.md).

## M2 authentication verification after the checkpoint

The expanded suite passed in Docker: **68 tests, zero failures/errors/skips**.
The existing M1 development database successfully applied V4 while preserving its
sample listing. The Docker HTTP demo then registered a new account, logged in,
verified session ID rotation and HttpOnly/SameSite=Lax cookie flags, renewed CSRF,
created/retrieved a listing owned by that account, checked 400/404 errors and logged
out. A subsequent current-user request returned 401. Both application services were healthy.
