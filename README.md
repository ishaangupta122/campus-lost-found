# Campus Lost and Found

Students report lost and found items, claim found items, and admins approve each claim before an item is handed back.

| Folder | What is in it |
|---|---|
| `backend/` | Spring Boot REST API (Java 21, PostgreSQL, JWT). See `backend/README.md` |
| `frontend/` | React + Vite + Tailwind app. See `frontend/README.md` |
| `docs/` | `API.md` (endpoints), `OOP-GUIDE.md`, `VIVA-NOTES.md` |

## Quick start

1. Create the database: `psql -U postgres -f backend/database/create-database.sql`
2. Copy `backend/.env.example` to `backend/.env` and fill it in.
3. Backend: `cd backend && mvn spring-boot:run` (port 8080). Tests: `mvn clean test`.
4. Copy `frontend/.env.example` to `frontend/.env`.
5. Frontend: `cd frontend && npm install && npm run dev` (http://localhost:5173).

Never commit `.env` files.
