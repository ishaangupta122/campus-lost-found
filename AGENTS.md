# Campus Lost and Found: guide for AI agents

Students report lost and found items, other students claim found items, an admin approves each claim, and the item is collected from the admin office. College project: keep code simple and easy to explain.

## Stack
- **Backend** `backend/`: Java 21, Spring Boot 3.5, Maven, PostgreSQL, JPA/Hibernate, Spring Security with JWT, Bean Validation. No Lombok. Port 8080.
- **Frontend** `frontend/`: React 19, Vite, Tailwind CSS v4, React Router 7, Axios, lucide-react. Port 5173 (fixed: the backend's CORS allows only this origin).
- **Docs** `docs/`: `WORKFLOW.md` (plain-language flow), `ARCHITECTURE.md` (folders and files), `BACKEND-GUIDE.md` (Java, Maven, Hibernate).

## Commands
| Task | Command |
|---|---|
| Create database (once) | `psql -U postgres -f backend/database/create-database.sql` |
| Run backend | `cd backend && mvn spring-boot:run` |
| Backend tests (129) | `cd backend && mvn clean test` (needs PostgreSQL and `backend/.env`; tests roll back) |
| Run frontend | `cd frontend && npm install && npm run dev` |
| Frontend build check | `cd frontend && npm run build` |
| Sample data | `psql -U <user> -d campus_lost_found -f backend/database/seed-items.sql` |

`backend/.env` (from `.env.example`): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (any text), `ADMIN_NAME/EMAIL/PASSWORD`, `GOOGLE_CLIENT_ID`; optional `CORS_ALLOWED_ORIGINS`, `UPLOAD_DIR`. `frontend/.env`: `VITE_API_BASE_URL`, `VITE_GOOGLE_CLIENT_ID`.

After changing code, run `mvn clean test` (backend) and `npm run build` (frontend).

## Backend (`backend/src/main/java/com/lostfound`)
Request path: `controller` -> `service` -> `repository` -> PostgreSQL.

- `Application`: entry point (`@EnableScheduling` lives here).
- `controller/`: Auth, Item, Claim, Admin, Upload, Health, plus the `CurrentUser` helper. HTTP only, no business rules.
- `service/`: all rules. `AuthService`, `ItemService`, `ClaimService`, `AdminService`, `JwtService`, `GoogleTokenVerifier`, `ImageStorageService`, `UploadCleanup` (nightly job).
- `repository/`: Spring Data interfaces and `ItemSpecifications` (dynamic search).
- `model/`: entities `User`, `Item`, `Claim` and enums `Role`, `ItemType`, `ItemStatus`, `ClaimStatus`.
- `dto/`: records: `AuthPayloads`, `ClaimRequests`, `ItemRequest`, `ItemResponse`, `ClaimResponse`, `UserResponse`, `DashboardResponse`.
- `config/`: `SecurityConfig` (URL rules, CORS, JSON 401/403), `JwtConfig`, `AdminBootstrap` (first admin from `.env`).
- `exception/`: `BadRequestException` (400), `ConflictException` (409), `ErrorResponse`, `GlobalExceptionHandler`. 404, 403 and 401 use `NoSuchElementException`, `AccessDeniedException` and `BadCredentialsException`.
- Tests mirror the main packages. `ApplicationTests` is the end-to-end flow.

## Frontend (`frontend/src`)
`pages/` (one per route), `components/`, `layouts/` (`MainLayout` navbar, `AdminLayout`), `routes/` (`AppRoutes`, `ProtectedRoute`), `contexts/`, `hooks/`, `services/` (`api.js` is the only Axios client), `utils/`, `styles/global.css` (theme colours).

- The JWT is stored in `localStorage` as `lf_token`. API errors arrive as `error.apiError { status, message, fieldErrors }`.
- Routes: public `/`, `/items/:id`, `/login`, `/register`; any user `/profile`; students `/report`, `/items/:id/edit`, `/my-items`, `/my-claims`; admins `/admin`, `/admin/claims`, `/admin/items`, `/admin/users` (linked from the navbar).

## Business rules to keep intact
- Item status: `OPEN` -> (claim) `CLAIM_PENDING` -> (admin approves, item collected) `RETURNED`. Rejecting the last waiting claim returns it to `OPEN`. `CLOSED` by owner or admin. Open and claim-pending items are the public list.
- Only `FOUND` items can be claimed. Several students may claim one item; approving one rejects the rest. Nobody claims their own item.
- **Admins cannot report or claim items.** Never add UI that offers it.
- An item with claims cannot be deleted (409): close it instead.
- Everyone who registers is a student. The only way to get an admin is `AdminBootstrap` from `.env`.
- Students must sign up and log in with a `@thapar.edu` email (form, backend `@Pattern` and `AuthService`, Google sign-in too). The admin can use any email.

## Conventions
**Backend:** constructor injection; DTO records, never entities, in the API; services are `@Transactional`; claim decisions lock the item row (`PESSIMISTIC_WRITE`); keep comments short and explain why, not what.

**Frontend UI style:** thin border plus small corners (`border border-line rounded-md`), cream surfaces, brick-red accent from the theme tokens (no hard-coded colours). Use `Panel` for boxes with a header strip. Form fields use the `bg-canvas` colour. Body text is at least `text-sm`. Button variants: `primary`, `secondary`, `soft`, `danger`, `dangerSoft`, `ghost`. Every page must work on a phone. Keep table cells on one line (truncate long text).

## Gotchas
- `spring.jpa.hibernate.ddl-auto=update` creates and alters tables on start. There are no migrations.
- Real credentials live only in `backend/.env` and `frontend/.env` (git-ignored). Never put real values in `.env.example`.
- `backend/uploads/` is git-ignored except `.gitkeep`.
- The local database holds real sample data (4 users, 10 items). When testing the UI, use throwaway accounts (`ui-...@example.com`) and delete every test user, item and claim afterwards.
- If behaviour changes, update `docs/WORKFLOW.md` and `docs/ARCHITECTURE.md`.

## Known limitations
No token revocation, no Google `nonce` check, pictures on local disk, `ddl-auto=update` instead of migrations.
