# Campus Lost and Found: Backend

Spring Boot REST API (Java 21, Maven, PostgreSQL, JWT) for reporting and claiming lost and found items.
More detail: [workflow](../docs/WORKFLOW.md), [architecture](../docs/ARCHITECTURE.md) and [backend guide](../docs/BACKEND-GUIDE.md).

## Prerequisites

- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -v`)
- PostgreSQL 14+ (`psql --version`)

## Setup

**1. Create the database** (once, from `backend/`):

```bash
psql -U postgres -f database/create-database.sql
```

On Homebrew installs your macOS username is often the superuser: use `psql -d postgres -f database/create-database.sql`.
The tables are created automatically when the app first starts.

**2. Create `.env`** from the template and fill it in (it is git-ignored):

```bash
cp .env.example .env
```

| Variable | Required | Meaning |
|---|---|---|
| `DB_URL` | yes | e.g. `jdbc:postgresql://localhost:5432/campus_lost_found` |
| `DB_USERNAME`, `DB_PASSWORD` | yes | PostgreSQL login (password may be empty) |
| `JWT_SECRET` | yes | Any text; signs login tokens. Keep it private |
| `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | no | First admin account, created at startup (password 8+ characters) |
| `GOOGLE_CLIENT_ID` | no | Enables Google sign-in; empty = disabled |
| `CORS_ALLOWED_ORIGINS` | no | Default `http://localhost:5173` |
| `UPLOAD_DIR` | no | Picture folder, default `uploads` |

Real environment variables override `.env`. Run Maven from `backend/` so the file is found.

**3. Run and check:**

```bash
mvn spring-boot:run                  # http://localhost:8080
curl http://localhost:8080/api/health
# {"status":"UP","database":"UP"}
```

Optional demo data: `psql -U <db_user> -d campus_lost_found -f database/seed-items.sql` loads 15 reports and hands them to the first 4 students (4, 4, 4, 3), including students who register later. `database/reset-data.sql` clears all reports and claims, keeps users, and reloads the seed.

## Tests

```bash
mvn clean test
```

123 tests. They need PostgreSQL running; database tests roll back, so no data is left behind.

## Project layout

```
backend/
  pom.xml
  database/              create-database.sql, seed-items.sql, reset-data.sql
  src/main/java/com/lostfound/
    Application.java     entry point
    controller/          REST endpoints (HTTP only)
    service/             business rules
    repository/          Spring Data JPA queries
    model/               entities and enums
    dto/                 request/response records
    config/              security, JWT, admin bootstrap
    exception/           custom exceptions + one global handler
  src/main/resources/    application.properties
  src/test/java/...      JUnit 5, Mockito, MockMvc
```

Request path: controller -> service -> repository -> database.

## Main dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST, JSON, embedded Tomcat |
| `spring-boot-starter-data-jpa` + `postgresql` | ORM and database driver |
| `spring-boot-starter-security` + `oauth2-resource-server` | Password hashing, JWT login, roles |
| `spring-boot-starter-validation` | Request validation |
| `spring-boot-starter-test`, `spring-security-test` | JUnit 5, Mockito, MockMvc |

## Further reading

[Workflow](../docs/WORKFLOW.md) · [Architecture](../docs/ARCHITECTURE.md) · [Backend guide](../docs/BACKEND-GUIDE.md)
