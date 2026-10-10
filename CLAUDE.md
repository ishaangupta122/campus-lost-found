# Campus Lost and Found — project context

Saved overview (2026-10-10). Students report lost/found items, others claim found items, an admin approves each claim, then the handover is confirmed.

## Stack
- **Backend** `backend/`: Spring Boot 3.5.16, Java 21, Maven, PostgreSQL, JPA/Hibernate (`ddl-auto=update`), Spring Security + JWT (HS256, oauth2-resource-server), optional Google sign-in, Bean Validation. No Lombok. Port 8080.
- **Frontend** `frontend/`: React 19, Vite 8, Tailwind v4, React Router 7, Axios, lucide-react. Port 5173 (strict; backend CORS only allows it).
- **Docs** `docs/`: `API.md` (endpoints), `OOP-GUIDE.md`, `VIVA-NOTES.md` — this is a college project with a viva; docs are written for explaining code.

## Run
1. `psql -U postgres -f backend/database/create-database.sql`
2. `backend/.env` (from `.env.example`): DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET (32+ chars), ADMIN_NAME/EMAIL/PASSWORD, GOOGLE_CLIENT_ID. Optional: JWT_EXPIRATION_MINUTES, CORS_ALLOWED_ORIGINS, UPLOAD_DIR, UPLOAD_ORPHAN_MAX_AGE_HOURS, UPLOAD_RETENTION_DAYS.
3. `cd backend && mvn spring-boot:run`; tests: `mvn clean test` (~129 @Test, needs real PostgreSQL for repository/integration tests).
4. `frontend/.env`: VITE_API_BASE_URL, VITE_GOOGLE_CLIENT_ID. Then `npm install && npm run dev`.

## Backend layout (`com.campus.lostandfound`)
- `model/`: `User`, `Item`, `Claim` + enums `Role`(STUDENT/ADMIN), `ItemType`(LOST/FOUND), `ItemStatus`(OPEN, CLAIM_PENDING active; RETURNED, CLOSED inactive), `ClaimStatus`(PENDING/APPROVED/REJECTED).
- `controller/`: Auth, Item, Claim, Admin, Upload, Health (+ `CurrentUser` helper). Thin, HTTP only.
- `service/`: AuthService, ItemService, ClaimService (row lock `PESSIMISTIC_WRITE` on item when deciding), AdminService, JwtService, GoogleTokenVerifier, ImageStorageService (magic-byte type check, UUID names, 5 MB), UploadCleanup (nightly @Scheduled), HealthService.
- `repository/`: JPA repos + `ItemSpecifications` (dynamic search by q/type/category/location/status).
- `dto/` (records), `exception/` (custom exceptions + `GlobalExceptionHandler` -> uniform JSON error), `config/` (SecurityConfig, JwtConfig, CorsConfig, AdminBootstrap creates first admin from env, JsonSecurityErrorHandler, SchedulingConfig).
- Security: public = register/login/google, health, GET items & item by id, GET upload files; `/api/admin/**` = ADMIN; everything else authenticated. Admins cannot report or claim items.
- Item lifecycle: OPEN -> (claim) CLAIM_PENDING -> (admin approves + handover confirmed) RETURNED; rejecting last pending claim -> OPEN; CLOSED by owner/admin. Delete blocked (409) if item has claims.
- Tests: controller (MockMvc), service (Mockito), repository, `FullFlowIntegrationTest`, exception handler, JWT, Google verifier, image storage.

## Frontend layout (`frontend/src`)
- `services/api.js`: single Axios client; attaches JWT from localStorage (`lf_token`); maps errors to `error.apiError {status,message,fieldErrors}`; 401 on protected call dispatches `auth:expired`.
- `contexts/` AuthContext, NotificationContext (toasts); `hooks/` useAuth, useFetch, useDebounced, useNotification, usePageTitle.
- `routes/AppRoutes.jsx` + `ProtectedRoute` (role guards). Routes: `/`, `/items/:id`, `/login`, `/register` (public); `/profile` (any user); `/report`, `/items/:id/edit`, `/my-items`, `/my-claims` (STUDENT); `/admin`, `/admin/claims`, `/admin/items` (ADMIN).
- `pages/` one per route; `components/` Button, Input, Select, ItemCard/Form/Filters, ImageUpload, ClaimCard, StatCard, StatusBadge/Steps, ConfirmDialog, GoogleButton, Skeletons, States, etc.; `layouts/` MainLayout, AdminLayout; theme tokens in `styles/global.css` (cream surfaces, brick-red primary, light-only).

## Misc
- Git: branch `master`, 2 commits ("init"), remote `origin`. `.github/modernize/java-upgrade/` is a tooling hook (ignore).
- `backend/uploads/` holds 2 sample uploaded images.
- `backend/README.md` (~31 KB) is a staged build log (Stages 2–7 + image upload) with curl examples and viva notes.

## Known limitations (from VIVA-NOTES)
No token revocation; no Google nonce; uploads on local disk; `ddl-auto=update` instead of migrations.

## Flags noticed on first scan
- **Uncommitted edits to both `.env.example` files** replaced the commented templates with concrete values (weak `JWT_SECRET`, an admin email/password). `.env.example` is committed, so if pushed these become public — consider reverting with `git checkout -- backend/.env.example frontend/.env.example` and keeping real values only in `.env` (gitignored).
- `backend/.gitignore` has no `uploads/` entry although `application.properties` says the upload folder is git-ignored.
