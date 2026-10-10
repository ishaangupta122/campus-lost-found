# Campus Lost and Found - Backend

Spring Boot REST API (Java 21, Maven, PostgreSQL) for reporting and claiming lost and found items on campus.

**Current status: all 8 stages complete.** `mvn clean test` passes. Remaining: your manual API/pgAdmin test pass and the Google client ID setup.

Extra guides: [API reference](../docs/API.md), [OOP guide](../docs/OOP-GUIDE.md), [viva notes](../docs/VIVA-NOTES.md).

| Stage | Topic | Status |
|---|---|---|
| 1 | Maven project and database setup | Done |
| 2 | Entities and repositories | Done |
| 3 | Item APIs | Written (see "Stage 3") |
| 4 | Authentication and authorization | Written (see "Stage 4") |
| 5 | Claim workflow | Written (see "Stage 5"); admin decision URLs are in Stage 6 |
| 6 | Admin APIs | Written (see "Stage 6") |
| 7 | Error handling and tests | Written (see "Stage 7") |
| 8 | Final docs and viva guide | Done (see `../docs/`) |

## Prerequisites

- JDK 21 (`java -version` must show 21)
- Maven 3.9+ (`mvn -v`)
- PostgreSQL 14+ (`psql --version`)

## 1. Create the database

macOS example (Homebrew): `brew install postgresql@16 && brew services start postgresql@16`.

```bash
cd campus-lost-found/backend
psql -U postgres -f database/create-database.sql    # runs: CREATE DATABASE campus_lost_found;
```
(On Homebrew installs your own macOS username is often the superuser: use `psql -d postgres -f ...` instead.)

## 2. Configure the database connection (.env file)

All database settings are read from a `.env` file in the `backend/` folder (next to `pom.xml`). Nothing is hardcoded in the code or in `application.properties`.

```bash
cp .env.example .env      # then edit .env with your own values
```

| Variable | Meaning |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/campus_lost_found` |
| `DB_USERNAME` | Your PostgreSQL username (pgAdmin: server > Properties > Connection) |
| `DB_PASSWORD` | Your PostgreSQL password; leave empty if none is required |
| `JWT_SECRET` | Secret used to sign login tokens, at least 32 random characters. Required. Generate: `python3 -c "import secrets; print(secrets.token_urlsafe(48))"` |
| `JWT_EXPIRATION_MINUTES` | Optional, token lifetime (default 60) |
| `GOOGLE_CLIENT_ID` | Optional, enables "Sign in with Google" (see Stage 4). Empty = disabled |
| `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | First admin account, created at startup (see Stage 4) |
| `CORS_ALLOWED_ORIGINS` | Optional, frontend origins allowed to call the API (default `http://localhost:5173`) |

How it works: `application.properties` contains `spring.config.import=optional:file:.env[.properties]`, which makes Spring Boot load `.env` as extra properties, and `spring.datasource.*` refer to them as `${DB_URL}` etc. Real OS environment variables (`export DB_USERNAME=...`) override `.env` if set. `.env` is listed in `.gitignore`; never commit it. Run Maven from the `backend/` folder so the file is found.

## 3. Run

```bash
mvn clean test              # unit test always runs; the context test needs PostgreSQL running
mvn spring-boot:run         # starts on http://localhost:8080
```

Check it:
```bash
curl http://localhost:8080/api/health
# {"status":"UP","database":"UP"}      -> server and PostgreSQL both reachable
# HTTP 503 {"status":"DEGRADED","database":"DOWN"} -> server up, database not reachable
```

## Folder structure

```
backend/
  pom.xml                         Maven build file and dependencies
  database/create-database.sql    one-time DB creation
  src/main/java/com/campus/lostandfound/
    CampusLostFoundApplication    main class (@SpringBootApplication)
    model/        entities + enums            (Stage 2)
    repository/   Spring Data JPA interfaces  (Stage 2)
    service/      business logic              (HealthService now)
    controller/   REST endpoints, HTTP only   (HealthController now)
    dto/          request/response objects    (Stage 3+)
    config/       security, JWT, CORS, admin bootstrap (Stage 4)
    exception/    custom exceptions + handler (Stage 7)
  src/main/resources/application.properties   configuration
  src/test/java/...                           JUnit 5 tests
```
Layering rule: controller -> service -> repository -> database. Controllers never contain business logic.

## Dependencies (pom.xml)

| Dependency | Why |
|---|---|
| `spring-boot-starter-parent` 3.5.16 | Manages compatible versions of everything else |
| `spring-boot-starter-web` | REST controllers, JSON, embedded Tomcat |
| `spring-boot-starter-data-jpa` | JPA/Hibernate: map Java classes to tables |
| `spring-boot-starter-security` | Password hashing, authentication, roles (Stage 4) |
| `spring-boot-starter-oauth2-resource-server` | Creates and verifies JWT login tokens (Nimbus); no custom crypto |
| `spring-boot-starter-validation` | `@NotBlank`, `@Email`, ... |
| `postgresql` (runtime) | JDBC driver |
| `spring-boot-starter-test`, `spring-security-test` | JUnit 5, Mockito, MockMvc |

Spring Boot 3.5 was chosen because most tutorials and documentation match it. It has reached end of open-source support; Spring Boot 4.1.x is the current line. For a college project 3.5 is fine, but upgrading means changing the version number and fixing some renamed starters/packages.

## Configuration notes

`spring.jpa.hibernate.ddl-auto=update`: at startup Hibernate compares entities with the database and creates missing tables/columns. It never drops data, but it cannot rename columns or migrate data safely, so production systems use Flyway/Liquibase. Fine for this project.

Security rules are in `SecurityConfig` (Stage 4).

## Stage 2: entities, enums and repositories

Tables are created automatically in `campus_lost_found` the first time the app starts (`ddl-auto=update`). Refresh the database in pgAdmin to see `users`, `items` and `claims`.

| Class | Table | Notes |
|---|---|---|
| `User` | `users` | unique `email`, `password_hash`, `role` (STUDENT/ADMIN), `created_at` |
| `Item` | `items` | one class for lost AND found (`type`), `status`, optional `image_path`, FK `reporter_id` -> users |
| `Claim` | `claims` | FK `item_id` -> items, FK `claimant_id` -> users, `message`, `status` |

Enums (`model/`): `Role`, `ItemType`, `ItemStatus` (OPEN, CLAIM_PENDING, RETURNED, CLOSED; each knows if it is "active"), `ClaimStatus`. Stored as text with `@Enumerated(EnumType.STRING)`, so the database shows readable values and reordering the enum cannot corrupt data.

Important JPA annotations:
- `@Entity`, `@Table(name = ...)`: this class is a table ("users" because `user` is reserved in PostgreSQL).
- `@Id` + `@GeneratedValue(IDENTITY)`: primary key generated by the database.
- `@Column(nullable, unique, length, name)`: column rules; `unique = true` on email gives the unique constraint.
- `@ManyToOne` + `@JoinColumn`: many items per user / many claims per item; creates the foreign key. `LAZY` loads the related row only when used.
- `@PrePersist`: sets `createdAt` automatically before the first insert.
- Protected no-argument constructor: required by JPA; the public constructor is what our code uses.

Repositories (`repository/`) extend `JpaRepository`, which provides save/find/delete for free. Methods such as `findByEmail` and `existsByItemIdAndClaimantIdAndStatus` have no body: Spring Data builds the SQL from the method name.

Stage 2 tests: `ItemTest` (plain unit tests) and `RepositoryTest` (runs against your real PostgreSQL inside a transaction that is rolled back, so no test data is left behind).

## Stage 3: item APIs

Item endpoints (create, list, search, filter, my reports, update, close, delete) with ownership rules and validation. Since Stage 4 the caller is identified by the login token (the temporary `X-User-Id` header and demo-user seeder from the first Stage 3 version were removed).

### Endpoints

Errors always look like `{"timestamp": "...", "status": 404, "error": "Not Found", "message": "Item 99 not found"}`; validation errors also contain `"fieldErrors": {"itemName": "Item name is required"}`. "Token" means the header `Authorization: Bearer <token>` (see Stage 4).

| Method and URL | Who | Request | Success | Common errors |
|---|---|---|---|---|
| `POST /api/items` | Student (token) | JSON below | 201 + item | 400 validation, 401 no/bad token, 403 admin |
| `GET /api/items` | Anyone | optional query: `q`, `type`, `category`, `location`, `status` | 200 + list of **active** items (OPEN, CLAIM_PENDING) newest first; with `status=` that status instead | 400 bad enum value |
| `GET /api/items/{id}` | Anyone | - | 200 + item | 404 |
| `GET /api/items/my` | Logged in | - | 200 + your reports | 401 |
| `PUT /api/items/{id}` | Owner or admin (token) | JSON below (no type/status) | 200 + item | 400, 401, 403 not owner, 404, 409 item returned/closed |
| `PUT /api/items/{id}/close` | Owner or admin (token) | - | 200 + item (status CLOSED) | 401, 403, 404, 409 already closed/returned |
| `DELETE /api/items/{id}` | Owner or admin (token) | - | 204 | 401, 403, 404, 409 item has claims (close it instead) |

Create body (`reportDate` and `imagePath` optional; `type` is `LOST` or `FOUND`):
```json
{
  "itemName": "Blue Backpack",
  "description": "Blue backpack with a red keychain",
  "category": "Bags",
  "type": "LOST",
  "location": "Main Library",
  "reportDate": "2026-10-09",
  "imagePath": null
}
```
Update body: the same without `type`. Response example:
```json
{
  "id": 7, "itemName": "Blue Backpack", "description": "Blue backpack with a red keychain",
  "category": "Bags", "type": "LOST", "location": "Main Library", "reportDate": "2026-10-09",
  "status": "OPEN", "imagePath": null, "reporterId": 4, "reporterName": "Alice Student",
  "createdAt": "2026-10-10T10:15:30.123"
}
```

### How it is built (viva notes)

- **Layers:** `ItemController` (HTTP only) -> `ItemService` (all rules) -> `ItemRepository` (database).
- **DTOs:** `ItemCreateRequest` / `ItemUpdateRequest` / `ItemResponse` are Java *records*. Clients cannot set `reporter`, `status` or `createdAt`, and cannot change `type` on update, because those fields do not exist in the request objects. Responses show only the reporter's id and name.
- **Validation:** Jakarta annotations (`@NotBlank`, `@Size`, `@NotNull`, `@PastOrPresent`) on the records, triggered by `@Valid` in the controller.
- **Ownership rule** (`requireOwnerOrAdmin`): students change only their own reports; admins may change any. Only students may create reports.
- **Search:** `ItemSpecifications` adds a condition only for filters that were supplied (keyword matches name or description; category is case-insensitive; location is a partial match).
- **Error handling:** custom exceptions (`ResourceNotFoundException` 404, `ForbiddenException` 403, `ConflictException` 409, `UnauthorizedException` 401) all extend `RuntimeException` (inheritance) and are converted to JSON by `GlobalExceptionHandler` (`@RestControllerAdvice`). Unexpected errors return a generic 500 and are logged on the server only.
- **Transactions:** `ItemService` is `@Transactional`; each method is one all-or-nothing unit of work.
- **Tests:** `ItemServiceTest` (mock repositories), `ItemControllerTest` (MockMvc with a fake token), `ItemSearchTest` (real PostgreSQL, rolled back).

## Stage 4: authentication and authorization

### How login works (explain this in the viva)

1. **Register or log in:** the client sends email and password to `/api/auth/register` or `/api/auth/login`.
2. **Password check:** passwords are never stored. Registration stores a **BCrypt hash** (`BCryptPasswordEncoder`: salted, deliberately slow, one-way). Login hashes the typed password the same way and compares.
3. **Token:** on success the server returns a **JWT** (JSON Web Token): a signed string holding the user id (`sub`), the `role`, and an expiry time. It is signed with `JWT_SECRET` (HS256) by Spring's built-in Nimbus library; we wrote no cryptography ourselves.
4. **Using it:** the client sends `Authorization: Bearer <token>` on every later request. A Spring Security filter verifies the signature and expiry, turns the `role` claim into `ROLE_STUDENT` / `ROLE_ADMIN`, and makes the user available to controllers. The server stores no session (stateless).
5. **Authorization:** URL rules in `SecurityConfig` (public vs logged in vs `/api/admin/**` admin only) are enforced before any controller runs. Business rules (e.g. "only the owner may edit") are enforced in the services.
6. **Failures:** no/invalid/expired token -> 401, wrong role -> 403, both as JSON (`JsonSecurityErrorHandler`).

Limitations to mention: a token stays valid until it expires (no logout list), and a role change only applies after the user logs in again.

### How the React frontend will use it (Phase 2)

```js
// login (or register) once
const res = await fetch("http://localhost:8080/api/auth/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email, password }),
});
const { token, user } = await res.json();
localStorage.setItem("token", token);          // keep the token

// every protected call
fetch("http://localhost:8080/api/items/my", {
  headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
});
```
CORS is already configured for `http://localhost:5173` (Vite's default; change with `CORS_ALLOWED_ORIGINS`). On a 401 the app should clear the token and show the login page. `GET /api/auth/me` returns the user for a stored token.

### Endpoints

| Method and URL | Who | Request | Success | Common errors |
|---|---|---|---|---|
| `POST /api/auth/register` | Anyone | `{"name","email","password"}` (password 8-72 chars) | 201 + `AuthResponse` | 400 validation, 409 email already used |
| `POST /api/auth/login` | Anyone | `{"email","password"}` | 200 + `AuthResponse` | 400 missing fields, 401 wrong email or password |
| `POST /api/auth/google` | Anyone | `{"idToken": "<Google ID token>"}` | 200 + `AuthResponse` | 400 missing token or Google login not configured, 401 invalid token / unverified email, 409 email already used by a password account |
| `GET /api/auth/me` | Logged in | - | 200 + user | 401 |

`AuthResponse`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": { "id": 4, "name": "Asha", "email": "asha@campus.edu", "role": "STUDENT" }
}
```
Emails are trimmed and lower-cased, so `Asha@Campus.edu` and `asha@campus.edu` are the same account. A `"role"` field in the register body is ignored: everyone who registers is a STUDENT.

### Creating the first admin

Put these in `.env` and start the app: `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` (8+ characters). `AdminBootstrap` creates that ADMIN account at startup if no user has that email yet; it never changes an existing account. Log in with those details to get an admin token. There is no API for becoming an admin.

### Try it with curl

```bash
# register a student (response contains the token)
curl -i -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Asha","email":"asha@campus.edu","password":"secret123"}'

# log in (copy the "token" value)
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"asha@campus.edu","password":"secret123"}'

TOKEN=paste-the-token-here
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
curl -i -X POST http://localhost:8080/api/items -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"itemName":"Blue Backpack","description":"Blue bag","category":"Bags","type":"LOST","location":"Library"}'
curl -i http://localhost:8080/api/items/my                       # 401: no token
```

### Sign in with Google (optional)

**How it works.** The React app shows Google's sign-in button. After the user picks their Google account, Google gives the browser an **ID token** (a signed JWT). The app sends it to `POST /api/auth/google`; the backend verifies it and returns *our own* token, exactly like a normal login, so everything after that is unchanged.

The backend (`GoogleTokenVerifier`) never trusts the frontend's claim. It checks that the token (1) has a valid signature from Google's public keys, (2) is not expired, (3) was issued by Google, (4) was issued for *our* app (`aud` equals `GOOGLE_CLIENT_ID`), and (5) has a verified email. Signature checking is done by Spring's Nimbus library.

**Accounts.** First sign-in creates a STUDENT account (name and email from Google, stored with Google's permanent user id in `users.google_id`). Later sign-ins find the user by that id. Google accounts have no usable password. If the email already belongs to an email/password account, Google sign-in is refused with 409 (the user logs in with the password instead). This stops someone taking over an account by registering another person's email first. Google can never create an admin.

**One-time setup (you do this in Google Cloud, free):**
1. Open https://console.cloud.google.com, create a project (e.g. "Campus Lost and Found").
2. APIs & Services > OAuth consent screen: choose External, enter an app name and your email, save. While the app is in "Testing", add your own Google account(s) under Test users.
3. APIs & Services > Credentials > Create credentials > OAuth client ID > Application type **Web application**.
4. Under **Authorized JavaScript origins** add `http://localhost:5173` (the React dev server). No redirect URI is needed for this flow.
5. Copy the **Client ID** (looks like `123-abc.apps.googleusercontent.com`) into `.env` as `GOOGLE_CLIENT_ID=...` and restart the backend. The client ID is public, not a secret; do not copy any "client secret", it is not used.

**Frontend (Phase 2) sketch,** using the `@react-oauth/google` package:
```jsx
<GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
  <GoogleLogin onSuccess={async (res) => {
    const r = await fetch("http://localhost:8080/api/auth/google", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken: res.credential }),
    });
    const { token, user } = await r.json();   // then store token as for normal login
  }} />
</GoogleOAuthProvider>
```

**Testing.** The unit tests cover the verification rules and account logic with fake tokens (no network). A real end-to-end test needs a real Google token, which only the frontend's Google button can produce, so it is tested in Phase 2. With `GOOGLE_CLIENT_ID` empty, `POST /api/auth/google` returns 400 "Google sign-in is not configured on this server". The server needs internet access to fetch Google's public keys.

Known limitations: no `nonce` check (an ID token stolen within its ~1 hour lifetime could be replayed; acceptable for this project), and no way to link a Google login to an existing password account.

### Files and concepts (viva notes)

- `config/SecurityConfig` (rules, BCrypt bean), `config/JwtConfig` (signing key, `JwtEncoder`, `JwtDecoder`), `config/JsonSecurityErrorHandler`, `config/CorsConfig`, `config/AdminBootstrap`, `service/JwtService`, `service/AuthService`, `controller/AuthController`.
- **Abstraction and polymorphism:** the code depends on interfaces (`PasswordEncoder`, `JwtEncoder`, `JwtDecoder`, `CommandLineRunner`) and Spring supplies the implementation (BCrypt, Nimbus). `JsonSecurityErrorHandler` implements two interfaces at once. `AdminBootstrap.run` and the handler methods use `@Override`.
- **Encapsulation:** `User` exposes the hash only to the service that needs it; `UserResponse` never contains it.
- **Dependency injection:** `AuthService` receives its encoder and token service through the constructor, so tests pass fakes or a real encoder easily.
- **Tests:** `AuthServiceTest` (hashing, duplicates, login), `JwtServiceTest` (real sign and verify, wrong secret rejected, short secret refused), `AuthControllerTest` (validation, 401).
- **Optional cleanup:** Stage 3 demo users (`alice@campus.test`, ...) may still be in your database with placeholder hashes. They cannot log in. Delete them in pgAdmin if you like (delete their items first).

## Stage 5: claim workflow

### The rules

| Rule | Result |
|---|---|
| Only students submit claims | admin gets 403 |
| Only items of type FOUND can be claimed | 400 |
| Returned or closed items cannot be claimed | 409 |
| You cannot claim your own report | 403 |
| One pending claim per student per item | 409 |
| An item with an approved claim cannot be claimed again | 409 |
| Several students may have pending claims on the same item at once | allowed; an admin picks one |
| Approving a claim automatically rejects all other pending claims of that item | one winner only |
| Rejecting the last waiting claim | item goes back to OPEN |
| Closing an item | its waiting claims are rejected |

### Item status life cycle

```
OPEN --(a student claims it)--> CLAIM_PENDING --(admin approves a claim)--> CLAIM_PENDING (waiting for handover)
                                                                                  |
                                              (finder or admin confirms handover) v
                                                                              RETURNED
Rejecting the last waiting claim: CLAIM_PENDING -> OPEN        Closing (owner/admin): any active status -> CLOSED
```
An approved claim means ownership is verified; the item becomes RETURNED only when the handover is confirmed.

### Endpoints (all need a token: `Authorization: Bearer <token>`)

| Method and URL | Who | Request | Success | Common errors |
|---|---|---|---|---|
| `POST /api/items/{id}/claims` | Student | `{"message": "Why it is mine (1-1000 chars)"}` | 201 + claim | 400 blank message / not a found item, 401, 403 admin or own item, 404, 409 duplicate / closed / already approved |
| `GET /api/claims/my` | Logged in | - | 200 + your claims, newest first | 401 |
| `GET /api/claims/{id}` | Claimant, the item's finder, or admin | - | 200 + claim | 401, 403, 404 |
| `PUT /api/items/{id}/returned` | The item's finder or admin | - | 200 + item (status RETURNED) | 401, 403, 404, 409 no approved claim yet |

Claim response:
```json
{
  "id": 5, "itemId": 10, "itemName": "Keys", "itemStatus": "CLAIM_PENDING",
  "claimantId": 2, "claimantName": "Claimant", "message": "It is mine, one key has a red tag",
  "status": "PENDING", "createdAt": "2026-10-10T11:02:10.456"
}
```
The admin operations (`GET /api/admin/claims/pending`, `PUT /api/admin/claims/{id}/decision`) are implemented and unit-tested in `ClaimService` (`listPendingClaims`, `decideClaim`); their URLs are added in Stage 6.

### Try it with curl (two students: the finder creates a FOUND item, the other claims it)

```bash
# FINDER reports a found item; note the id
curl -s -X POST $BASE/api/items -H "$H" -H "Authorization: Bearer $TOKEN_A" \
  -d '{"itemName":"Keys","description":"Two keys on a ring","category":"Keys","type":"FOUND","location":"Canteen"}'

# CLAIMANT claims it (item becomes CLAIM_PENDING)
curl -i -X POST $BASE/api/items/ITEM/claims -H "$H" -H "Authorization: Bearer $TOKEN_B" -d '{"message":"Mine, one key has a red tag"}'
curl -s $BASE/api/claims/my -H "Authorization: Bearer $TOKEN_B"
curl -i -X POST $BASE/api/items/ITEM/claims -H "$H" -H "Authorization: Bearer $TOKEN_B" -d '{"message":"again"}'   # 409
curl -i -X POST $BASE/api/items/ITEM/claims -H "$H" -H "Authorization: Bearer $TOKEN_A" -d '{"message":"mine"}'    # 403 own item
```

### How it is built (viva notes)

- **`ClaimService`** holds all rules; **`ClaimController`** only translates HTTP.
- **Transactions (`@Transactional`):** a claim and its item always change together. For example, approving updates the claim, rejects the other claims and keeps the item status in one all-or-nothing unit. If any step fails, everything is rolled back.
- **Row locking (`findByIdForUpdate`, `PESSIMISTIC_WRITE`):** methods that change an item first lock its database row. Two admins approving different claims for the same item at the same moment are processed one after the other, so two approvals can never both succeed.
- **Composition:** a `Claim` has an `Item` and a claimant `User`.
- **Visibility rule:** a claim can be read by the claimant, the person who found the item (they must see who is collecting it) and admins.
- **Tests:** `ClaimServiceTest` (all rules and status changes, fake repositories), `ClaimControllerTest` (HTTP codes and validation), plus repository tests against PostgreSQL.

## Stage 6: admin APIs

Every `/api/admin/**` URL needs a token whose role is ADMIN. This is enforced twice: by the URL rule in `SecurityConfig` and again inside the services (defence in depth). A student token gets 403.

### Endpoints (header `Authorization: Bearer <admin token>`)

| Method | URL | What it does |
|--------|-----|--------------|
| GET | `/api/admin/dashboard` | Counts: users, lost/found items, items per status, pending claims |
| GET | `/api/admin/claims/pending` | Claims waiting for a decision, oldest first |
| PUT | `/api/admin/claims/{id}/decision` | Body `{"decision":"APPROVED"}` or `{"decision":"REJECTED"}` |
| GET | `/api/admin/items` | All items including RETURNED and CLOSED; optional `q`, `type`, `category`, `location`, `status` |
| PUT | `/api/admin/items/{id}/close` | Close any item (waiting claims are rejected) |
| PUT | `/api/admin/items/{id}/returned` | Confirm the handover, item becomes RETURNED |

Dashboard response example:

```json
{ "totalUsers": 5, "totalLostItems": 3, "totalFoundItems": 4, "openItems": 4,
  "claimPendingItems": 1, "returnedItems": 1, "closedItems": 1, "pendingClaims": 1 }
```

### Try it with curl

```bash
curl -s $BASE/api/admin/dashboard -H "Authorization: Bearer $ADMIN_TOKEN"
curl -s $BASE/api/admin/claims/pending -H "Authorization: Bearer $ADMIN_TOKEN"
curl -i -X PUT $BASE/api/admin/claims/CLAIM/decision -H "$H" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"decision":"APPROVED"}'
curl -s "$BASE/api/admin/items?status=CLOSED" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -i $BASE/api/admin/dashboard -H "Authorization: Bearer $TOKEN_B"   # 403 for a student
```

### How it is built (viva notes)

- **`AdminController`** has no logic; it calls `AdminService` (dashboard) and the existing `ClaimService` / `ItemService`, so rules are not duplicated (reuse).
- **Role-based access:** `hasRole("ADMIN")` on `/api/admin/**` plus `requireAdmin` checks in services.
- **Dashboard** uses `count...` queries, not loading every row.
- **Tests:** `AdminServiceTest`, `AdminControllerTest` (401/403/200 cases), and an admin-list test in `ItemServiceTest`.

## Stage 7: error handling and tests

### Error format (same for every failure)

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "fieldErrors": { "itemName": "Item name is required" } }
```

| Situation | Status |
|-----------|--------|
| Invalid body, bad id format, missing header, malformed JSON | 400 |
| Missing, expired or invalid token / wrong password | 401 |
| Logged in but not allowed (student on admin URL, not your item) | 403 |
| Unknown item, claim or URL | 404 |
| Duplicate email, duplicate claim, item in wrong state | 409 |
| Unexpected bug | 500 with a generic message (details only in the server log) |

401 and 403 raised by Spring Security (before a controller runs) use the same JSON through `JsonSecurityErrorHandler`.

### Test layers (explain this in the viva)

| Layer | Classes | Uses |
|-------|---------|------|
| Unit (services, entities) | `ItemServiceTest`, `ClaimServiceTest`, `AuthServiceTest`, `AdminServiceTest`, `JwtServiceTest`, `GoogleTokenVerifierTest`, `HealthServiceTest`, `ItemTest` | Mockito fakes, no database |
| Error handler | `GlobalExceptionHandlerTest` | A tiny fake controller throws each exception; checks status and JSON |
| Web layer | `*ControllerTest` | `@WebMvcTest`, fake services, real security rules |
| Repository | `RepositoryTest`, `ItemSearchTest` | Real PostgreSQL, rolled back |
| End to end | `FullFlowIntegrationTest` | Whole app + PostgreSQL: register, login, report, claim, approve, return, plus 401/403/404/409 cases; rolled back |

Run everything with `mvn clean test`. Tests never leave data behind.

## Image upload (local disk)

Pictures are stored on the server's own disk (folder `UPLOAD_DIR`, default `backend/uploads/`, git-ignored), never in the database or on an outside service. A report stores only the picture's URL path in its existing `imagePath` field, so the database did not change.

| Method | URL | Access | Notes |
|---|---|---|---|
| POST | `/api/uploads` | logged in | multipart form, field `file`; returns `{"imagePath": "/api/uploads/files/<uuid>.png"}` |
| GET | `/api/uploads/files/{name}` | public | the picture (public so an `<img>` tag can show it) |

Then send that `imagePath` when you create or edit a report.

**Rules (ImageStorageService):** JPG, PNG, GIF or WebP only, up to 5 MB. The real type is detected from the file's first bytes (not the file name or the Content-Type the client claims). The stored name is a random UUID, so a client cannot pick a name or folder. Errors: 400 wrong type or empty, 413 too large.

**Temporary storage (UploadCleanup, every night at 03:00):**
1. a picture no report uses (uploaded but never saved) is deleted once it is older than `UPLOAD_ORPHAN_MAX_AGE_HOURS` (24);
2. the picture of a RETURNED or CLOSED report older than `UPLOAD_RETENTION_DAYS` (30) is deleted and the report's `imagePath` is cleared. Set it to `0` to keep them forever.

**Note on Multer:** Multer is a library for Node.js/Express. This backend is Spring Boot, so the same job is done with Spring's built-in multipart support (`MultipartFile`) and plain `java.nio` file calls.

**Viva notes:** `MultipartFile` is Spring's object for an uploaded file; `@Scheduled` + `@EnableScheduling` run the nightly job; the file type is verified with "magic numbers" because the client's claim cannot be trusted.

## Concepts used so far (viva notes)

- **Dependency injection (constructor-based):** `HealthController` receives `HealthService`, which receives `DataSource`. Spring builds and passes them in; no `new` in our code, and tests can pass fakes.
- **Encapsulation:** fields are `private final`, set only through constructors.
- **Layered design / separation of concerns:** HTTP in controllers, logic in services.
- **Exception handling:** `HealthService` catches `SQLException` and turns it into a simple true/false.
- **Composition:** `Item` has a `User` reporter; `Claim` has an `Item` and a `User` claimant.
- **Constructor overloading:** `Item` has two constructors (with and without a report date).
- **Enums with data/methods:** `ItemStatus.isActive()`.
- **Unit testing with mocks:** `HealthServiceTest` fakes the `DataSource`, so it needs no database.

## Verification status

- Stage 1: confirmed by the project owner: `GET /api/health` returns `{"status":"UP","database":"UP"}` against a local PostgreSQL.
- Stage 2: confirmed by the project owner: tests pass and the `users`, `items`, `claims` tables exist in pgAdmin.
- Stages 3 and 4: `mvn clean test` passes (61 tests); basic manual checks done by the project owner.
- Stage 5: code written; run `mvn clean test`, then the curl examples.
- Stage 6: code written; run `mvn clean test`, then the admin curl examples with the admin token.
- Stage 7: tests written; run `mvn clean test`.
- Stages 5 to 7: `mvn clean test` passes (confirmed by the project owner). Manual API testing is still to be done.
- Stage 8: documentation only.
