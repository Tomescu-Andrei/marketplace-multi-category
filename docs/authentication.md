# M2 authentication foundation

## Design and endpoints

Spring Security authenticates username-or-email/password through a jOOQ-backed UserDetailsService
and BCrypt with cost 12. Sessions are saved in HttpSessionSecurityContextRepository,
rotate their ID on login, expire after 30 idle minutes and are invalidated on logout.
Cookies are HttpOnly, SameSite=Lax, with cookie-only session tracking. Local HTTP
uses secure=false; configure SESSION_COOKIE_SECURE=true for HTTPS.

| Endpoint | Access | Result |
|---|---|---|
| GET `/api/v1/auth/csrf` | Public | Session-bound CSRF token and headerName |
| POST `/api/v1/auth/register` | Public, CSRF required | 201 account; does not log in |
| POST `/api/v1/auth/login` | Public, CSRF required | 200 account and authenticated session |
| GET `/api/v1/auth/me` | Authenticated | 200 account; otherwise 401 |
| POST `/api/v1/auth/logout` | CSRF required | 204, invalidates session and clears cookie |

Account responses contain only id, username, email, displayName and createdAt. Hashes and
passwords are never returned. Emails are validated and stored lowercase; a database
lower(email) unique index prevents case-variant duplicates, including concurrent
registration races. Passwords require 12–72 characters and at most 72 UTF-8 bytes,
matching BCrypt's byte limit. Display names are nonblank, at most 100 characters,
and stripped before persistence.

Registration also requires a unique username of 3–32 ASCII characters, starting
with a letter/digit and otherwise allowing letters, digits, dot, underscore and
hyphen. Usernames are stored lowercase and protected by a case-insensitive unique
index. The @ character is excluded so email and username namespaces cannot overlap.

Login accepts `{"identifier":"admin","password":"..."}` or an email in the same
identifier field. Leading/trailing identifier whitespace is stripped, and lookup
is case-insensitive. The old JSON `email` key remains an alias for existing clients.
After lookup the security principal remains the canonical email, preserving account
resolution and existing session semantics whichever login identifier was supplied.

Unknown accounts, users without passwords and incorrect passwords produce generic
401 UNAUTHENTICATED errors. Duplicate registration returns generic 409 DATA_CONFLICT.
Invalid fields return 400 VALIDATION_FAILED. Email verification is not implemented.

## CSRF and browser integration

Fetch GET `/api/v1/auth/csrf` before POST operations, keeping the same session cookie.
Send the returned token unchanged in its named header (`X-CSRF-TOKEN`). The default
Spring XOR handler supplies a masked token. Fetch it again after login because the
old token is invalidated; after logout, fetch a token for the new anonymous session.
Tokens from another session are rejected. Token responses are not cached.

Missing/invalid tokens return 403 INVALID_CSRF_TOKEN before MVC validation. Listing
creation without authentication but with a valid CSRF token returns 401. Public
category/listing reads, Swagger and health remain available. Other routes require
authentication unless explicitly permitted by SecurityConfig.

The React client uses same-origin requests through Nginx in Docker or a Vite proxy
in native development. No JWT or browser token storage is required;
global cross-origin access is not enabled. Sessions currently live in the backend
process, so restart logs users out while accounts and listings persist in PostgreSQL.
Shared session storage remains necessary before multi-instance deployment.

## Ownership, migration and verification

ListingService resolves the authenticated account from Spring Security and the
database within the creation transaction. The request cannot specify sellerId.
Existing listings retain their original seller. User IDs appear on listings;
seller account records, emails and password hashes are not exposed there.

V4 adds nullable password_hash and the case-insensitive email index. M1 users are
preserved with null hashes and cannot log in. New registration always supplies a
BCrypt hash. Applied migrations are not rewritten, and jOOQ regenerates UsersRecord.

V5 adds usernames without changing account IDs, emails or password hashes. Existing
accounts receive user_<id>; a valid unique display name is retained as the username
when it does not collide with another account's name/fallback. Thus the local admin
account receives admin. No administrative role is implied by a username.

The Docker demo uses a real HTTP cookie jar for registration, login, renewed CSRF,
authenticated creation, retrieval and logout. Integration tests also check hashing,
duplicate email behavior, session ID rotation and cross-session token rejection.

The React authentication interface is implemented; see [frontend](frontend.md).
Listing pages, editing/deletion with ownership checks and category filtering remain
M2 work. Password reset, verified email, login throttling and production session
storage are outside this first authentication increment.

References: [session management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html),
[CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html),
[authentication persistence](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html).
