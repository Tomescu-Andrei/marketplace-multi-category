# Verification and demonstration

## Automated verification

Requirements: Java 21, Maven 3.9+ and Docker Desktop with Linux containers running.
From `backend/`, run:

```powershell
mvn clean verify
```

The build migrates a temporary PostgreSQL instance, generates jOOQ sources,
compiles the application and runs the tests. A separate integration database is
created by Testcontainers. Its data is reset between test cases and is unrelated
to the Compose development database. Tests explicitly select the local profile.
No mocked repository replaces PostgreSQL in integration tests.

| Area | Evidence |
|---|---|
| Database lifecycle | Clean migrations, validation and no-op re-migration |
| jOOQ | Generated schema types compile as part of a clean build |
| Categories | Seeded categories and category-specific attribute definitions |
| Listings | 201/Location, persisted values, retrieval and money precision |
| Pagination | Bounds, offsets and stable ordering for tied creation timestamps |
| Input validation | Required fields, size and money boundaries, attribute type/required/code checks |
| Parsing | Fractional IDs, numeric enum values, unknown fields, duplicate keys and trailing JSON rejected |
| Errors | Field details, 400/404/405/415 consistency and preserved Allow header |
| Database safeguards | Foreign keys and fixed-price/positive-price checks reject invalid direct inserts |
| OpenAPI | All five operations, creation example, error schemas and Location header |
| Swagger UI | Redirect, HTML, JavaScript asset and API-docs configuration served |
| Internal failures | Focused MVC tests confirm generic 409/500 messages without SQL or exception disclosure |

Tests are in `MarketplaceApplicationTests` (real PostgreSQL) and
`GlobalExceptionHandlerTests` (isolated MVC failure-path tests). Surefire reports
are written to `backend/target/surefire-reports/` and are not committed.

## Manual M1 demonstration

1. From the repository root, start PostgreSQL with `docker compose up -d --wait`.
2. From `backend/`, run `mvn spring-boot:run`. Set `SERVER_PORT` first if 8080 is occupied.
3. Inspect tables with:

   ```powershell
   docker compose exec postgres psql -U marketplace -d marketplace -c "\dt"
   ```

   Run this Compose command from the repository root.
4. Explain the controller → service → repository → PostgreSQL flow using
   [architecture](architecture.md) and [database design](database-design.md).
5. Open `http://localhost:8080/swagger-ui.html` (adjust the port if needed).
6. Execute GET categories, then GET details for category 1 to inspect attribute definitions.
7. Under POST listings, click **Try it out**, keep the supplied valid example and execute.
   Expect 201, an ID, timestamps and a Location header. The write persists in the local database.
8. Retrieve that ID with GET listings/{id}, then list all listings.
9. Remove the price or change the numeric `year` to a string. Expect 400 with a field error.
10. Fetch an absent ID to show 404. Show the successful clean build/test summary.

The API definition is available as JSON at `/v3/api-docs` and YAML at `/v3/api-docs.yaml`.
The default local profile provides the seller; no login is needed for this M1 demo.
This is not the final authentication design. AUCTION stores metadata only.

## Troubleshooting

- Docker unavailable: start Docker Desktop with Linux containers, then rerun the build.
  Tests intentionally fail rather than skip database checks.
- Port conflict: PostgreSQL uses host port 15432. If this is occupied, change the
  Compose mapping and `DB_URL` together. Change backend port with `SERVER_PORT`.
- Missing generated types in IntelliJ: run `mvn generate-sources` and reload the Maven project.
- Flyway checksum mismatch: restore the applied migration and introduce a new version;
  do not edit history or reset a database containing data you need.
- Changed local password: changing an environment variable does not change the
  password inside an existing database volume; update the database credentials deliberately.

## M1 acceptance coverage

The implemented scope covers startup, PostgreSQL, Flyway, generated jOOQ compilation,
category/listing endpoints, validation/errors, Swagger UI and integration tests.
Architecture, database decisions, research proposal and startup/demo instructions
are documented. The research proposal is a plan; datasets, models and experimental
results remain future work. There is no frontend, authentication, bidding, payment,
AI inference or production cloud deployment in M1.
