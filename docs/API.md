# API reference

Base URL `http://localhost:8080`. Send JSON with `Content-Type: application/json`.
Protected URLs need `Authorization: Bearer <token>` (token comes from login). Every error uses the same JSON:
`{ "timestamp", "status", "error", "message", "fieldErrors" }` (`fieldErrors` only on validation errors).

Access: **Public** = no token, **Student** = any logged-in user, **Admin** = ADMIN role only.

## Health
| Method | URL | Access | Notes |
|---|---|---|---|
| GET | `/api/health` | Public | `{"status":"UP","database":"UP"}` |

## Authentication
| Method | URL | Access | Body / result |
|---|---|---|---|
| POST | `/api/auth/register` | Public | `{name, email, password(8-72)}` creates a STUDENT; 409 if email exists |
| POST | `/api/auth/login` | Public | `{email, password}` returns `{token, tokenType, expiresInSeconds, user}`; 401 on wrong details |
| POST | `/api/auth/google` | Public | `{idToken}` Google sign-in; 401 invalid token, 409 email belongs to a password account |
| GET | `/api/auth/me` | Student | The logged-in user |

## Items
| Method | URL | Access | Notes |
|---|---|---|---|
| POST | `/api/items` | Student | `{itemName, description, category, type(LOST/FOUND), location, reportDate?, imagePath?}`; status starts OPEN |
| GET | `/api/items` | Public | Active items (OPEN, CLAIM_PENDING). Filters: `q`, `type`, `category`, `location`, `status` |
| GET | `/api/items/my` | Student | My own reports, newest first |
| GET | `/api/items/{id}` | Public | One item; 404 if missing |
| PUT | `/api/items/{id}` | Owner | Edit details (type and status cannot be changed here) |
| PUT | `/api/items/{id}/close` | Owner | Status CLOSED; waiting claims are rejected |
| DELETE | `/api/items/{id}` | Owner | 409 if the item has any claims |

## Image upload
| Method | URL | Access | Notes |
|---|---|---|---|
| POST | `/api/uploads` | Student | multipart field `file` (JPG/PNG/GIF/WebP, max 5 MB); returns `{imagePath}`; 400 wrong type, 413 too large |
| GET | `/api/uploads/files/{name}` | Public | the stored picture |

Send the returned `imagePath` in the `imagePath` field when creating or editing an item.

## Claims
| Method | URL | Access | Notes |
|---|---|---|---|
| POST | `/api/items/{itemId}/claims` | Student | `{message}`; 403 own item, 409 duplicate or item not claimable; item becomes CLAIM_PENDING |
| GET | `/api/claims/my` | Student | My claims |
| GET | `/api/claims/{id}` | Claimant, item reporter, Admin | One claim |
| PUT | `/api/items/{itemId}/returned` | Reporter or Admin | Confirms handover; item becomes RETURNED (needs an approved claim) |

## Admin (`/api/admin/**`, ADMIN only; students get 403)
| Method | URL | Notes |
|---|---|---|
| GET | `/api/admin/dashboard` | Counts of users, lost/found items, items per status, pending claims |
| GET | `/api/admin/users` | All registered users (id, name, email, role, googleAccount, createdAt), newest first; no password data |
| GET | `/api/admin/claims/pending` | Waiting claims, oldest first |
| PUT | `/api/admin/claims/{id}/decision` | `{"decision":"APPROVED"}` or `REJECTED` |
| GET | `/api/admin/items` | All items, any status; filters `q`, `type`, `category`, `location`, `status` |
| PUT | `/api/admin/items/{id}/close` | Close any item |
| PUT | `/api/admin/items/{id}/returned` | Confirm handover |

## Status codes
200/201 success, 400 invalid input, 401 not logged in or bad token, 403 not allowed, 404 not found, 409 conflict (duplicate or wrong state), 500 unexpected.

## Item status life cycle
`OPEN` -> (claim submitted) `CLAIM_PENDING` -> (admin approves, handover confirmed) `RETURNED`.
Rejecting the last waiting claim returns the item to `OPEN`. `CLOSED` is set by the owner or an admin.
