# Verification and demonstration

## Automated verification

Requirements: Docker Desktop with Linux containers running. From the repository root:

```powershell
docker compose --profile tools run --build --rm verify
```

The Maven container migrates a temporary PostgreSQL instance, generates jOOQ sources,
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
| Authentication | Registration, BCrypt storage, case-insensitive email uniqueness, generic login failures |
| Session/CSRF | Session ID rotation, saved login, token renewal, cross-session rejection and logout |
| Seller identity | Authenticated account supplies sellerId; anonymous creation is rejected |

Tests are in `MarketplaceApplicationTests` (real PostgreSQL) and
`GlobalExceptionHandlerTests` (isolated MVC failure-path tests). Surefire reports
are written to `backend/target/surefire-reports/` and are not committed.

## Manual M1 demonstration

1. From the repository root, start everything with `docker compose up -d --build --wait`.
2. Run `docker compose ps` to confirm PostgreSQL and backend are healthy.
3. Inspect tables with:

   ```powershell
   docker compose exec postgres psql -U marketplace -d marketplace -c "\dt"
   ```

   Run this Compose command from the repository root.
4. Explain the controller → service → repository → PostgreSQL flow using
   [architecture](architecture.md) and [database design](database-design.md).
5. Open `http://localhost:8081/swagger-ui.html` (adjust BACKEND_PORT if needed).
6. Execute GET categories, then GET details for category 1 to inspect attribute definitions.
7. Follow [the authentication flow](authentication.md) to register, log in and renew
   the CSRF token. Under POST listings, click **Try it out**, set X-CSRF-TOKEN,
   keep the supplied valid example and execute.
   Expect 201, an ID, timestamps and a Location header. The write persists in the local database.
8. Retrieve that ID with GET listings/{id}, then list all listings.
9. Remove the price or change the numeric `year` to a string. Expect 400 with a field error.
10. Fetch an absent ID to show 404. Show the successful clean build/test summary.

The API definition is available as JSON at `/v3/api-docs` and YAML at `/v3/api-docs.yaml`.
M2 now requires login for creation. Public reads remain accessible. AUCTION stores metadata only.

The repeatable HTTP demo runs with `docker compose --profile tools run --rm demo`.
It verifies the cookie/session flow and leaves a demo account/listing in the local database.

## Troubleshooting

- Docker unavailable: start Docker Desktop with Linux containers, then rerun the build.
  Tests intentionally fail rather than skip database checks.
- Port conflict: PostgreSQL uses host port 15432. If this is occupied, change the
  Compose mapping through POSTGRES_PORT (and native DB_URL if used). Change the
  Compose backend mapping with BACKEND_PORT.
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
results remain future work. M1 had no frontend, authentication, bidding, payment,
AI inference or production deployment. M2 has now started with authentication;
see [the M1 checkpoint report](m1-verification.md) for the preceding clean-clone evidence.
