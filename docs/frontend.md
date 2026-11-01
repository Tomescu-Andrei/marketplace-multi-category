# React authentication interface

## Scope and routes

The frontend uses React with JavaScript, Vite, Tailwind CSS and React Router.
Manrope fonts are bundled locally; no external font/image service is required.
The interface is in Romanian and supports desktop and narrow/mobile layouts.

The header theme button switches between light and dark palettes on every route.
It follows the system color scheme until the user chooses a theme, then saves only
that preference in localStorage under piata-theme. The choice survives reload and
syncs across tabs. An external pre-paint script applies the palette before React
loads, preserving the Content Security Policy. The toggle also works when browser
storage is unavailable, without persisting the choice.

| Route | Behavior |
|---|---|
| `/` | Redirects to login or account depending on the restored server session |
| `/register` | Registration, password confirmation and field validation |
| `/login` | Login, invalid-credential feedback and registration/logout notices |
| `/cont` | Current account details and logout; anonymous visitors redirect to login |
| Other routes | Not-found message and link back to the entry point |

Registration sends username, email, displayName and password. Confirmation is checked
locally and is not sent to the backend. After success the login form is pre-filled
with the username, while password fields are cleared. Login uses a text field for
either username or email, not an email-only input. Registration does not silently
establish a logged-in session. The authenticated account page shows only the public
account DTO, never a password/hash. Credentials exist only in form memory.

Labels, autocomplete, field descriptions, invalid states, first-error focus and
visible keyboard focus are provided. Passwords can be revealed/hidden explicitly.
Submit controls indicate pending requests and prevent duplicate submission.
Email/name/password checks complement authoritative backend validation, including
the BCrypt UTF-8 byte limit. Server errors are presented in Romanian without raw
SQL, stack traces or internal messages.

## Sessions and CSRF

The API client requests `/api/v1/auth/me` at startup. A 401 means an anonymous user;
network/server failures show a retry screen rather than pretending the user logged
out. A logged-in session survives page reload through the HttpOnly cookie.
On window focus, an existing account is rechecked; an expired session redirects to
login with an explanatory notice. Request revision tracking prevents a delayed
account refresh from restoring a user after logout.

Each mutation first fetches `/api/v1/auth/csrf` and sends the returned session-bound
token unchanged in its named header. This also handles token rotation after login
and logout. Requests use same-origin credentials. Neither tokens nor passwords
are saved in localStorage/sessionStorage. Failed logout leaves the account view
with a retryable error; the UI does not claim the server session was terminated.

## Docker and proxy

```powershell
docker compose up -d --build --wait
docker compose --profile tools run --build --rm frontend-verify
```

The frontend is at `http://localhost:8082` by default (FRONTEND_PORT override).
The Node 22 build stage runs npm ci from the committed lockfile and Vite's production
build. Only dist/ is copied into the unprivileged Nginx runtime on internal port 8080.
No Node/npm process is required on the host or in the serving container.

Nginx serves SPA deep links and proxies `/api/` to backend:8080 without rewriting
the API path. Docker DNS is re-resolved so backend replacement does not require
a frontend rebuild. Cookies are returned through the same origin; global CORS is
not enabled. CSP limits scripts/styles/fonts/connections to the app's own origin.
The frontend has its own serving health check and initially waits for backend health.

An optional native workflow uses Node 22: npm ci, then npm run dev from frontend/.
Vite proxies `/api` to localhost:8081, overridable via API_PROXY_TARGET. Production
Docker traffic uses the Nginx proxy instead. Rebuild the frontend image after source changes.

## Verification

Vitest and React Testing Library test registration, confirmation, error focus,
password reveal, login/logout, account restoration, backend outage retry, delayed
refresh/logout races, CSRF renewal and money-independent password byte boundaries.
API mocks are used for isolated client/component tests; the browser check uses the
real Docker-hosted backend/database and Nginx cookie proxy.

The interactive check registered a disposable local account, rejected an incorrect
password, logged in, reloaded the account URL, logged out and verified that direct
account navigation required login again. The test account remains in the development
database. No existing account credentials were changed.

Username support is validated against the real API: new usernames are unique and
case-insensitive, and existing email-based login remains supported. The account
screen displays username separately from display name and email.

## Remaining M2 work

Marketplace listing pages, category filtering, listing detail UI, creation forms,
editing/deletion and ownership checks on those future mutations are separate
increments. This interface implements authentication/account access only.
