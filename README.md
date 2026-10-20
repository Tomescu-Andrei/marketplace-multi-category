# marketplace-multi-category

Multi-category marketplace developed for the Research Industry Projects course.

## Current status

Initial backend project only: Java 21, Spring Boot, Maven, Spring Web,
Validation and JUnit 5. The application starts without a database.

PostgreSQL, Flyway, jOOQ, Docker Compose, Swagger and marketplace endpoints
will be added incrementally during M1. No frontend is implemented.

## Run locally

Requirements: JDK 21 and Maven 3.9 or newer.

From the repository root:

```powershell
cd backend
mvn spring-boot:run
```

The server listens on `http://localhost:8080`. No API endpoints exist yet;
a 404 response at `/` is expected. Stop the application with Ctrl+C.

## Verify and package

From `backend/`:

```powershell
mvn verify
java -jar target/marketplace-backend-0.0.1-SNAPSHOT.jar
```

The startup test loads the application with an embedded web server on a random port.

See [AGENTS.md](AGENTS.md) for the architecture and milestone requirements.
