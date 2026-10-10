# Viva preparation

## 30-second summary
A Spring Boot REST backend where students report lost or found items, other students claim found items, and an admin approves claims and confirms the return. Data is in PostgreSQL, access is controlled by JWT login (email/password or Google), and the code is split into controller, service, repository and model layers.

## Architecture in one picture
```
Client -> Security filter (checks JWT) -> Controller (HTTP only)
       -> Service (rules, transactions) -> Repository (JPA) -> PostgreSQL
Errors: any layer throws -> GlobalExceptionHandler -> clean JSON
```

## Likely questions and short answers
**Why layers?** Each layer has one job; controllers have no business logic, so rules can be tested without HTTP.
**What is a DTO and why use one?** A plain object for request/response data. It stops clients setting fields like role or status.
**How does login work?** Password is checked with BCrypt; the server signs a JWT (HS256) containing user id and role; the client sends it in the `Authorization` header; a filter validates it on every request. No server-side session (stateless).
**Why BCrypt?** One-way, salted, slow hash; passwords are never stored or returned.
**How is admin protected?** URL rule `/api/admin/**` needs ROLE_ADMIN, and services check the role again. Register ignores any role field; the first admin is created from `.env`.
**What is JPA / Hibernate?** Maps classes to tables; repositories give save/find without SQL. `ddl-auto=update` creates tables.
**What is `@Transactional`?** Several database changes succeed or fail together.
**How do you stop two approvals for one item?** The item row is locked (`PESSIMISTIC_WRITE`) inside the transaction, so a second admin waits.
**How does search work?** JPA `Specification` builds the query from only the filters supplied.
**Why are secrets in `.env`?** Nothing sensitive is in source code; `.env` is gitignored, `.env.example` is the template.
**How does Google sign-in work?** The frontend gets a Google ID token and posts it; the backend verifies signature, issuer, audience and verified email, then finds or creates the user.
**How did you test?** Unit tests with Mockito fakes, web-layer tests with MockMvc, repository tests on real PostgreSQL, and one end-to-end flow test; all roll back.
**How does image upload work?** The browser sends the file as `multipart/form-data` to `POST /api/uploads`. Spring gives it to us as a `MultipartFile`; we check size and the real file type from its first bytes, save it under a random UUID name in a folder on the server, and return its URL path. The report stores only that path. A nightly `@Scheduled` job deletes unused uploads and old pictures of finished reports. (Multer is the Node.js equivalent; Spring has this built in.)
**Which HTTP codes and when?** See [API.md](API.md) (400, 401, 403, 404, 409, 500).

## Be honest about limitations
- A token stays valid until it expires (no revocation); a role change needs re-login.
- Google tokens are not checked with a nonce.
- Pictures live on the server's disk, so they are lost if the server's disk is wiped and are not shared between several servers (a real deployment would use cloud storage).
- `ddl-auto=update` is fine for a project, but real systems use migrations (Flyway).

## Demo order (5 minutes)
1. `GET /api/health`. 2. Register two students, log in. 3. Student A reports a FOUND item. 4. Student B claims it (status CLAIM_PENDING). 5. Show student getting 403 on `/api/admin/dashboard`. 6. Admin approves, marks returned, shows dashboard. 7. Show tables in pgAdmin. 8. Run `mvn clean test`.
