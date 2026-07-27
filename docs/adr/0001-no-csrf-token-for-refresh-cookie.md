# No CSRF token on the refresh-token cookie

The refresh token is stored as an httpOnly `SameSite=Strict` cookie, and the app is deliberately same-origin
with the API (Vite dev proxy locally, nginx reverse-proxy planned for prod). `SameSite=Strict` prevents the
cookie from being attached to any cross-site request, which already closes the classic CSRF vector for
`POST /api/v1/auth/refresh` — so we're not adding a separate CSRF token (double-submit cookie or synchronizer
token) on top of it. Revisit this if the deployment story ever moves the frontend to a different origin than
the API, since that would force `SameSite=Lax`/`None` and remove this protection.
