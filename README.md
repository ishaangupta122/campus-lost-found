# Campus Lost and Found

Students report lost and found items and claim found ones. An admin approves each claim, and the handover is confirmed.

| Folder | Contents |
|---|---|
| [`backend/`](backend/README.md) | Spring Boot 3.5, Java 21, PostgreSQL, JWT |
| [`frontend/`](frontend/README.md) | React 19, Vite, Tailwind CSS |
| [`docs/`](docs/) | Workflow, architecture, backend guide |

## Quick start

1. Create the database: `psql -U postgres -f backend/database/create-database.sql`
2. Backend: copy `backend/.env.example` to `backend/.env` and edit it, then `cd backend && mvn spring-boot:run` (http://localhost:8080).
3. Frontend: copy `frontend/.env.example` to `frontend/.env`, then `cd frontend && npm install && npm run dev` (http://localhost:5173).

Tests: `cd backend && mvn clean test` (PostgreSQL must be running). Never commit `.env` files.

Start with [docs/WORKFLOW.md](docs/WORKFLOW.md) to see how the app works, then [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the folders and files.
