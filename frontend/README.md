# Campus Lost and Found: Frontend

React single-page app (Vite, Tailwind CSS) for the Spring Boot backend in [`../backend`](../backend/README.md).

## Prerequisites

- Node.js 20.19+ or 22.12+ (`node -v`)
- The backend running on http://localhost:8080

## Setup

**1. Create `.env`** from the template (it is git-ignored):

```bash
cp .env.example .env
```

| Variable | Required | Meaning |
|---|---|---|
| `VITE_API_BASE_URL` | yes | Backend address, default `http://localhost:8080` |
| `VITE_GOOGLE_CLIENT_ID` | no | Enables Google sign-in; must equal `GOOGLE_CLIENT_ID` in `backend/.env`. Empty = button disabled |

**2. Install and run:**

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # production build in dist/
```

Keep port 5173: the backend's CORS allows only that origin (`CORS_ALLOWED_ORIGINS`).

## Pages

| URL | Who | What |
|---|---|---|
| `/` | everyone | Search, Lost/Found switch, filters, item cards |
| `/items/:id` | everyone | Details; students claim found items; owners and admins manage the report |
| `/login`, `/register` | guests | Email and password, or Google |
| `/report`, `/items/:id/edit` | students | Create or edit a report, with photo upload |
| `/my-items`, `/my-claims` | students | Own reports and claims |
| `/profile` | any user | Account details and recent activity |
| `/admin` | admins | Counts, items by status, waiting claims, latest reports |
| `/admin/claims` | admins | Approve or reject pending claims |
| `/admin/items` | admins | All items with status tabs; close or mark returned |
| `/admin/users` | admins | All users |

Admins cannot report or claim items (the backend forbids it). Admin pages are linked from the navbar.

## Project layout

```
src/
  main.jsx, App.jsx    entry point and providers
  routes/              AppRoutes, ProtectedRoute (login and role guards)
  layouts/             MainLayout (navbar), AdminLayout (loads dashboard numbers)
  pages/               one file per route
  components/          shared UI (Button, Panel, ItemCard, ItemForm, ClaimCard, ...)
  contexts/            AuthContext, NotificationContext (toasts)
  hooks/               useAuth, useFetch, useDebounced, useNotification, usePageTitle
  services/            api.js (one Axios client) + auth, item, claim, admin, upload calls
  utils/               constants, date and label formatting, image URLs
  styles/global.css    Tailwind import and theme colours
```

## How it works

- **API client** (`services/api.js`): one Axios instance. It adds `Authorization: Bearer <token>` and turns every error into `error.apiError = { status, message, fieldErrors }`.
- **Login:** the JWT is kept in `localStorage` (`lf_token`). On load `AuthContext` calls `/api/auth/me` to restore the user, and a 401 on a protected call logs the user out.
- **Route guards:** `ProtectedRoute` redirects guests to `/login` and wrong roles to their home page. This only hides pages; the backend enforces real access.
- **Forms:** backend `fieldErrors` appear under the matching field; other errors show in a box or a toast.
- **Photos:** `ImageUpload` posts to `POST /api/uploads` and keeps the returned path in the form.
- **Styling:** Tailwind classes in the components. Colours (cream surfaces, brick-red accent) are defined once in `styles/global.css`. The site is light mode only.

## Google sign-in (optional)

Create a Web OAuth client in Google Cloud, add `http://localhost:5173` as an authorized JavaScript origin, and put the same client ID in both `.env` files.
