# Architecture

How the project is organised: which folder holds what, and what each file is for.

## The project at a glance

```
campus-lost-found/
  backend/        the server: Java, Spring Boot, PostgreSQL
  frontend/       the website: React
  docs/           documents
  README.md       how to set up and run everything
```

| Part | What it is |
|---|---|
| `backend/` | A Spring Boot server. It stores data in PostgreSQL and answers requests from the website with JSON. |
| `frontend/` | A React website that runs in the browser and talks to the backend. |
| `docs/` | `WORKFLOW.md` (how the app works for users), `ARCHITECTURE.md` (this file) and `BACKEND-GUIDE.md` (Java, Maven and Hibernate: the backend tools and concepts). |

## How the frontend and backend work together

1. The website is a separate app on http://localhost:5173. The backend runs on http://localhost:8080.
2. The website never touches the database. It asks the backend for everything by sending requests, and the backend replies with JSON.
3. After login the backend gives the website a token. The website sends that token with every later request so the backend knows who is asking.
4. The website shows different pages and buttons for guests, students and admins. The backend repeats these checks, so hiding a page is only for convenience.
5. If something goes wrong, the backend sends back one standard error message and the website shows it next to the form field or in a notice.
6. Photos are uploaded first. The backend saves the file and returns its address, and the website sends that address along with the report.

## Backend

```
backend/
  pom.xml                       Maven build file
  .env / .env.example           private settings / template
  database/                     SQL scripts
  uploads/                      pictures saved by the app (not in git)
  target/                       build output (generated)
  src/
    main/java/com/lostfound/    the application code
    main/resources/             settings
    test/java/com/lostfound/    the tests
```

| Item | Use |
|---|---|
| `pom.xml` | Lists the libraries (dependencies), the Java version and the build plugin. Maven reads it. |
| `.env` | Your private settings (database login, secret, admin account). Never committed. |
| `.env.example` | The same file with placeholders, used as a template. |
| `database/` | `create-database.sql` creates the empty database. `seed-items.sql` loads 15 sample reports and assigns them to the first 4 students, now or when they register (database trigger). `reset-data.sql` deletes all reports and claims (users are kept) and reloads the seed. |
| `uploads/` | Where uploaded pictures are stored. Only an empty `.gitkeep` is tracked. |
| `target/` | Created by Maven when it builds. Safe to delete. |

### `src/main/resources/`
| File | Use |
|---|---|
| `application.properties` | Server settings: database connection, secret, upload limits and the admin account. Values come from `.env`. |

### `src/main/java/com/lostfound/`
Requests flow through the folders in this order: **controller, service, repository, database**.

| File | Use |
|---|---|
| `Application.java` | The starting point. Running it starts the whole server. |

#### `controller/` receives web requests and sends replies
Controllers only handle the web side. They check the request shape and pass the work to a service.

| File | Use |
|---|---|
| `AuthController` | Register, log in (also with Google), and "who am I". |
| `ItemController` | Create, list, search, view, edit, close and delete items. |
| `ClaimController` | Submit a claim, list my claims, view one claim, mark an item returned. |
| `AdminController` | Dashboard numbers, user list, pending claims, approve or reject, all items. |
| `UploadController` | Upload a picture and serve a stored picture. |
| `HealthController` | A quick "is the server and database up?" check. |
| `CurrentUser` | Small helper that reads the logged-in user's id from the token. |

#### `service/` holds the rules of the app
Services decide what is allowed, and they use the repositories to read and save data.

| File | Use |
|---|---|
| `AuthService` | Sign-up and login rules. |
| `JwtService` | Creates the login token. |
| `GoogleTokenVerifier` | Checks that a Google sign-in is genuine. |
| `ItemService` | Rules for reports: who may edit, close or delete, and how searching works. |
| `ClaimService` | Rules for claims: who may claim, what approving and rejecting do, and marking returned. |
| `AdminService` | Dashboard counts and the user list. |
| `ImageStorageService` | Checks, saves and serves uploaded pictures. |
| `UploadCleanup` | A nightly job that removes pictures nobody needs. |

#### `repository/` reads and saves data in the database
Repositories are interfaces. Spring creates the working code for them.

| File | Use |
|---|---|
| `UserRepository` | Find and save users, for example by email. |
| `ItemRepository` | Find and save items, and count them for the dashboard. |
| `ClaimRepository` | Find and save claims. |
| `ItemSpecifications` | Builds the search query from whichever filters were used. |

#### `model/` the data the app stores
Each entity class is one database table. Enums are fixed lists of values.

| File | Use |
|---|---|
| `User` | A person: name, email, role. Table `users`. |
| `Item` | A lost or found report. Table `items`. |
| `Claim` | A request to receive a found item. Table `claims`. |
| `Role` | `STUDENT` or `ADMIN`. |
| `ItemType` | `LOST` or `FOUND`. |
| `ItemStatus` | `OPEN`, `CLAIM_PENDING`, `RETURNED`, `CLOSED`. |
| `ClaimStatus` | `PENDING`, `APPROVED`, `REJECTED`. |

#### `dto/` the shapes of data sent in and out
DTO stands for **Data Transfer Object**. DTOs are small, read-only objects (Java records) that carry data in and out of the API. They control exactly which fields a client can send or see.

| File | Use |
|---|---|
| `AuthPayloads` | Login, register and Google-login requests, and the login response. |
| `ClaimRequests` | The body of a claim and of an admin's decision. |
| `ItemRequest` | The body for creating or editing a report. |
| `ItemResponse` | An item as sent to the website. |
| `ClaimResponse` | A claim as sent to the website. |
| `UserResponse` | A user as sent to the website (never the password). |
| `DashboardResponse` | The admin dashboard numbers. |

#### `config/` setup that runs at start
| File | Use |
|---|---|
| `SecurityConfig` | Who may open which URL, how login tokens are read, and which website may call the server. |
| `JwtConfig` | Sets up creating and checking login tokens. |
| `AdminBootstrap` | Creates the first admin account from `.env` when the server starts. |

#### `exception/` how errors become replies
| File | Use |
|---|---|
| `BadRequestException` | "The request is wrong" (HTTP 400). |
| `ConflictException` | "That clashes with the current state" (HTTP 409). |
| `ErrorResponse` | The standard shape of every error reply. |
| `GlobalExceptionHandler` | Catches errors anywhere and turns them into that standard reply. |

### `src/test/java/com/lostfound/`
The test folders mirror the main code, and each test class is named after what it tests (`ClaimServiceTest` tests `ClaimService`).

| Folder or file | Tests |
|---|---|
| `ApplicationTests` | The whole app end to end: sign up, report, claim, approve, return. |
| `controller/` | The web layer: status codes, validation and who may call what. |
| `service/` | The rules, using fake repositories. |
| `repository/` | Database queries against the real PostgreSQL (rolled back afterwards). |
| `exception/` | That each error becomes the right reply. |

## Frontend (in brief)
A React website. Pages are for browsing and searching, reporting items, claiming, and the student and admin areas. It calls the backend as described above and holds no data of its own beyond the login token. Setup is in `frontend/README.md`.

## Short extras

**Request path:** website, then controller, service, repository, PostgreSQL, and the reply comes back the same way.

**Database tables:**

| Table | Holds |
|---|---|
| `users` | name, email, password (stored scrambled), role, created date |
| `items` | name, description, category, type, location, date, status, picture path, who reported it |
| `claims` | which item, which student, their message, status |

**Run and test:**
```bash
cd backend && mvn spring-boot:run      # server on :8080
cd frontend && npm run dev             # website on :5173
cd backend && mvn clean test           # all tests (PostgreSQL must be running)
```
