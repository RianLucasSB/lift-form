# Liftform

Liftform is a gym-movement analyzer: users upload lift videos that are scored for form quality by a
computer-vision pipeline.

## Language

**Session**:
The authenticated period for a user, backed by an Access Token (short-lived, held in memory) and a Refresh
Token (long-lived, rotated on each use, stored as an httpOnly cookie).

**Access Token**:
A short-lived (24h) JWT identifying the authenticated user, sent as `Authorization: Bearer`. Held only in
memory on the frontend — never persisted to `localStorage`/`sessionStorage` — and reissued via a refresh.

**Refresh Token**:
A long-lived (10-day), single-use credential stored as an httpOnly `SameSite=Strict` cookie. Exchanging it
for a new Access Token also rotates it — the old value is revoked and a new one issued in the same call.
_Avoid_: "refresh cookie" (the cookie *holds* the Refresh Token; not synonyms in docs/logs)

**Login** (field):
The identifier a user submits to sign in — either their email or their username, whichever they typed.
Named `login` end-to-end (backend DTO field and frontend payload field) to keep it distinct from "the login
flow" or "the login page".
_Avoid_: "identifier" (name is deliberately kept as `login`, matching the API contract 1:1)

**Processing** (frontend label):
The single label the frontend shows while a `VideoAnalysis` is anywhere between upload and a terminal
state. Deliberately collapses three distinct backend statuses (`CREATED`, `UPLOADED`, `PROCESSING`) into one
UI-facing word, since the distinction between them isn't meaningful to someone watching a spinner.
_Avoid_: using "Processing" to mean specifically the `PROCESSING` status — in frontend code/copy it always
means the collapsed range, not the single enum value.

**Classification**:
The `GOOD`/`FAIR`/`POOR` quality verdict for a completed analysis, computed on the backend from
`overallScore` via `Classification.fromScore(...)` — the one source of truth for "what counts as a good
score." This is the only quality label the frontend renders as the headline result.
_Avoid_: confusing this with `feedback.overall_label` — a separate, finer-grained
(`excellent`/`good`/`fair`/`poor`/`bad`) label computed by the Python scoring worker for its own internal
use. The two use different thresholds on the same `overallScore` and are not interchangeable; see
`docs/adr/0004-result-view-shows-classification-not-overall-label.md`.
