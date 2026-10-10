# Campus Lost and Found - Frontend

React + Vite single-page app for the Spring Boot backend in `../backend`.
Stack: React, React Router DOM, Axios, Tailwind CSS v4, Lucide icons, Inter font (self-hosted with `@fontsource-variable/inter`).

## Requirements
Node.js 20.19+ or 22.12+ (Vite 8). Check with `node -v`.

## Run
```bash
cd frontend
cp .env.example .env     # check VITE_API_BASE_URL (default http://localhost:8080)
npm install
npm run dev              # http://localhost:5173
```
Start the backend first. Use port 5173: the backend only allows that origin through CORS.
Build for production with `npm run build`.

## Pages
| URL | Who | What |
|---|---|---|
| `/` | everyone | hero, how it works (guests), live search with Lost/Found switch and filters, item grid with photos |
| `/items/:id` | everyone | details; students can claim found items; owners and admins manage the report |
| `/login`, `/register` | guests | email/password forms and "Continue with Google" |
| `/report`, `/items/:id/edit` | students | create and edit reports (type cards, category list, photo upload) |
| `/my-items`, `/my-claims` | students | my reports (edit, close, delete) and my claims |
| `/admin` | admins | attention panel, clickable counts, items-by-status bar, waiting claims, latest reports |
| `/admin/claims` | admins | pending claims with age, approve or reject (tab shows the waiting count) |
| `/admin/items` | admins | table of all items, status tabs with counts, filters kept in the URL, close or mark returned |

Admins cannot report or claim items (the backend does not allow it), so those pages and buttons are student-only.

## Photos
`ImageUpload` sends the chosen picture to `POST /api/uploads` (see the backend README) and keeps the returned path in the form. Pictures are shown with `utils/image.js`, which turns a stored path into a full URL. Older reports that stored a web link still display.

## Structure
```
src/
  main.jsx, App.jsx        entry point and providers
  styles/global.css        Tailwind import + design tokens (@theme) + a few base rules
  contexts/                AuthContext (user, login, logout), NotificationContext (toasts)
  hooks/                   useAuth, useNotification, useFetch (load data), useDebounced, usePageTitle
  services/                api.js (one Axios client) + auth/item/claim/admin services
  components/              Button, Input, Select, StatusBadge, ItemCard, ItemFilters, ItemForm,
                           ClaimCard, StatCard, ImageUpload, SegmentedControl, StatusSteps, Skeletons, ConfirmDialog, GoogleButton, States, ...
  layouts/                 MainLayout (navbar, footer), AdminLayout (admin tabs)
  routes/                  AppRoutes, ProtectedRoute (login and role guards)
  pages/                   one file per page
  utils/format.js          date and label formatting
```

## Styling
Tailwind classes in the components do the styling. `global.css` holds only what Tailwind cannot express as classes: the color tokens (`@theme`: cream surfaces, brick red `primary`, earthy status colors), heading sizes and the focus outline. Changing the theme means editing those tokens. The site is light-only.

## Google sign-in (optional)
1. Create a Web OAuth client in Google Cloud and add `http://localhost:5173` as an authorized JavaScript origin.
2. Put the client ID in `frontend/.env` as `VITE_GOOGLE_CLIENT_ID` and in `backend/.env` as `GOOGLE_CLIENT_ID` (the same value).
3. Restart both. Until then the button is shown disabled.

## How it works (viva notes)
- **One Axios client** (`services/api.js`): base URL from `VITE_API_BASE_URL`, adds `Authorization: Bearer <token>`, and turns every error into `error.apiError = { status, message, fieldErrors }` (the backend's error JSON).
- **Auth:** the JWT is kept in `localStorage`. On page load `AuthContext` calls `/api/auth/me` to restore the user. A 401 on a protected call logs the user out.
- **Route guards:** `ProtectedRoute` sends guests to `/login` (and back afterwards) and wrong roles to their home page. This only hides pages; the backend enforces real access.
- **Forms:** backend `fieldErrors` are shown under the matching field; other errors appear in a red box or a toast.
- **Contexts only for shared state:** auth and notifications. Each page keeps its own data.
