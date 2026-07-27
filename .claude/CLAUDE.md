# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Liftform is a gym-movement analyzer. A user uploads a video of a lift (currently only squats); the video is
processed by a computer-vision pipeline to extract rep-level pose features, which are then scored for form
quality. The system is a polyglot monorepo with three parts that communicate over RabbitMQ and S3-compatible
object storage:

- `api/liftform` — Java 21 / Spring Boot 4 REST API (source of truth for users, auth, and video-analysis records)
- `workers/pose-feature-extractor-worker` — Python worker that consumes upload events, downloads the video, runs
  MediaPipe pose estimation, and computes rep/angle features
- `workers/score-analyzer-worker` — Python worker (model training + inference) that consumes extracted-feature
  events off RabbitMQ, turns them into a 0–1 form score and per-dimension feedback, and publishes the result (or a
  structured error) for the API to persist
- `frontend` — React 19 + TypeScript SPA (Vite, Tailwind CSS v4, shadcn/ui, React Router). Landing page, sign-up and
  sign-in forms wired to the register/login APIs, session bootstrap + proactive token refresh, and a protected
  `/overview` area logged-in users land in; the upload flow comes next

## Commands

### API (run from `api/liftform/`)

```bash
./mvnw test                                    # unit tests only
./mvnw failsafe:integration-test failsafe:verify  # integration tests (Testcontainers: Postgres, LocalStack S3, RabbitMQ; needs Docker)
./mvnw test -Dtest=CreateAnalysisUseCaseImplTest  # single unit test class
./mvnw spring-boot:run -Plocal                 # run API locally (activates `local` Spring profile)
```

CI (`.github/workflows/ci.yml`) only triggers on changes under `api/**`. It runs `./mvnw test` on every push/PR to
`main`, and `./mvnw failsafe:integration-test failsafe:verify` only on pushes to `main` (not PRs), after unit tests
pass.

### Full stack (docker-compose, run from repo root)

```bash
docker compose up            # base (rabbitmq, postgres, minio) + docker-compose.override.yml (spring-api, pose-feature-extractor-worker, score-analyzer-worker, frontend) — used for local dev, loaded automatically
docker compose -f docker-compose.yml -f docker-compose.prod.yml up   # prod-style compose, images pulled from ECR instead of built locally
```

`docker-compose.override.yml` is picked up automatically by `docker compose` alongside `docker-compose.yml`, which
is why local dev only needs `docker compose up`.

The `frontend` service in `docker-compose.override.yml` is dev-only: `frontend/Dockerfile` installs deps and runs
`pnpm run dev` (Vite dev server, not a production build), with the source directory bind-mounted back in (plus an
anonymous volume over `node_modules` so the container's own install — matching its Linux/glibc environment — isn't
shadowed by whatever `node_modules` exists on the host). `vite.config.ts`'s dev-server proxy target is driven by
`API_PROXY_TARGET` (falls back to `http://localhost:8080` for running `pnpm run dev` natively outside Docker), set
to `http://spring-api:8080` in compose so the container reaches the API by service name; the proxy is still what
keeps the app and API same-origin (see the frontend auth notes below), same as running the frontend natively. There
is deliberately no production image/target yet — how the built bundle gets served and how `/api` gets reverse-proxied
in prod (nginx in the image vs. path-based routing at an ALB, etc.) is still an open decision.

### Frontend (run from `frontend/`)

```bash
pnpm install
pnpm run dev        # Vite dev server on :5173; proxies /api → http://localhost:8080 (vite.config.ts)
pnpm run build      # tsc -b + vite build
pnpm run lint       # oxlint
```

Frontend notes:

- **Package manager is pnpm, not npm** — migrated because npm has a long-standing bug (npm/cli#4828) silently
  skipping platform-native optional dependencies (the `@rolldown/*` / `@oxlint/*` binding packages that `vite
  build` and `oxlint` need). Use `pnpm`, not `npm`, for all installs/scripts in this directory; there's no
  `package-lock.json` anymore, only `pnpm-lock.yaml`.
- **Deliberately a Vite SPA, not Next.js** — the Spring API owns auth/business logic, and the refresh token is an
  httpOnly `SameSite=Strict` cookie (see `AuthController`), which requires the app and API to be same-origin: the
  Vite dev proxy handles this locally; production will use nginx serving the bundle + reverse-proxying `/api`
  (not yet wired into docker-compose).
- Path alias `@/` → `src/` (configured in both `vite.config.ts` and the tsconfigs — `paths` without `baseUrl`,
  which TS 6 deprecates). shadcn/ui components are generated into `src/components/ui/` via `npx shadcn add <name>`
  (config in `components.json`); don't hand-write those.
- Design tokens live in `src/index.css` (`:root` brand palette: `--plate-red`, `--plate-blue`, `--pass-green`,
  `--steel`, mapped into shadcn's `--primary`/`--background`/etc.). Display type is Archivo Variable's width axis
  (`font-stretch-expanded` + `font-black`), data/labels are IBM Plex Mono — both self-hosted via `@fontsource`.
- `package.json` still pins `@rolldown/binding-linux-x64-gnu` and `@oxlint/binding-linux-x64-gnu` as exact-version
  `devDependencies` (not `optionalDependencies`) — pnpm's own automatic platform-optional-dependency resolution
  turned out to have the identical failure mode in this dev environment (reproduced with an unrelated package,
  `esbuild`, in isolation — not an npm-specific bug after all, something about this machine's optional-dep
  detection). Pinning the exact package+version sidesteps whatever auto-detection is failing, for either package
  manager. If `vite build` or `oxlint` fails with "Cannot find native binding" again (e.g. after a version bump
  changes the pinned version), reinstall after deleting `node_modules` and `pnpm-lock.yaml`, or extract the binding
  tarball manually via `pnpm pack`/`npm pack` and drop it into `node_modules/@scope/binding-.../`.

### Pose feature extractor worker (`workers/pose-feature-extractor-worker/`)

```bash
pip install -r requirements.txt
python listener.py     # connects to RabbitMQ and consumes video-analysis.queue
```

Requires `RABBITMQ_URL` and AWS/S3 env vars (see `.env` in that directory); against local dev stack these point at
the `minio` and `rabbitmq` docker-compose services.

### Score analyzer worker (`workers/score-analyzer-worker/`)

```bash
pip install -r requirements.txt
python listener.py     # connects to RabbitMQ and consumes video-analysis.scoring.queue
```

Requires `RABBITMQ_URL` and `MODEL_VERSION` (see `.env` in that directory). Unlike the extractor worker, this one
never touches S3 — it only consumes an already-extracted feature dict from the message body.

## Architecture

### API: hexagonal / clean architecture with an explicit transactional decorator layer

Package layout under `com.rianlucassb.liftform`:

- `core/domain` — framework-free domain model (`VideoAnalysis`, `User`, `RefreshToken`, domain events, domain
  exceptions). `VideoAnalysis` is a rich domain object with private constructors; instances are created via
  `initialize(...)` (new record) or `reconstitute(...)` (rehydrated from persistence), and state transitions
  (e.g. `confirmUpload()`) are validated internally (`InvalidStatusTransitionException`) rather than left to callers.
- `core/gateway` — interfaces the domain/use-case layer depends on (`VideoAnalysisRepository`, `VideoStorage`,
  `EventPublisher`, `UserRepository`, security ports). No Spring/JPA/AMQP types leak in here.
- `core/usecases/<area>/<action>/` — one use case per folder, always as an interface (`XUseCase`) + input/output
  records + a plain `XUseCaseImpl` that takes gateways via constructor injection and has **no Spring annotations**.
  This keeps use cases unit-testable with plain mocks (see `src/test/.../core/usecases/**`).
- `infraestructure/adapter/*` — concrete implementations of the gateway interfaces: JPA repositories/mappers under
  `persistence`, `RabbitMQEventPublisher` under `messaging`, `S3VideoStorage` under `storage`, JWT/password hashing
  under `auth`.
- `infraestructure/adapter/transaction/*` — a decorator layer: e.g. `TransactionalCreateAnalysisUsecase` wraps the
  plain `CreateAnalysisUseCaseImpl` with `@Transactional`. Wiring happens in `infraestructure/config/usecase/*Config`
  classes, which register the plain impl under a qualifier (e.g. `"createVideoAnalysisImpl"`) and expose the
  transactional wrapper as the `@Primary` bean that controllers actually receive. **When adding a new use case that
  needs a transaction boundary, follow this same wrap-and-mark-@Primary pattern** rather than annotating the use
  case impl directly — it's what keeps the core use-case classes framework-free.
- `presentation` — `@RestController`s, request/response DTOs, mappers (MapStruct) from use-case output to DTO, and
  `GlobalExceptionHandler` for translating domain exceptions to HTTP responses. All routes are versioned under
  `ApiPaths.V1` (`/api/v1`).

Security: stateless JWT auth (`JWTSecurityFilter` populates `@AuthenticationPrincipal JWTUserData`), configured in
`SecurityConfig`. Only `/api/v1/auth/**` is public; everything else requires authentication. Refresh tokens are
stored hashed (`RefreshTokenHasher`) with their own repository/entity, separate from access tokens (which are JWTs,
not persisted). Access-token TTL is `security.jwt.access-token-ttl-seconds` (`application.yml`, defaults to `86400`
if unset) — `TokenService` (implements `AccessTokenGenerator`) is the single place that reads it, both to set the
JWT's `exp` claim and to expose it via `expiresInSeconds()`, which `Login`/`Register`/`RefreshTokenUseCaseImpl` all
call to populate `expiresIn` on their outputs (and, in turn, `LoginResponseDTO`/`RegisterResponseDTO`/
`RefreshTokenResponseDTO`) — the frontend uses this to schedule its own proactive refresh instead of decoding the
JWT itself.

Migrations: Flyway, `src/main/resources/db/migration/V*.sql`, applied in order — add new changes as new `V{n}__*.sql`
files, never edit an already-applied migration.

### Video analysis lifecycle & messaging

`VideoAnalysisStatus` moves `CREATED → UPLOADED → PROCESSING → COMPLETED` (or `FAILED`/`EXPIRED`), enforced by
`VideoAnalysis`'s transition methods. Flow:

1. `POST /api/v1/analysis/create` (`CreateAnalysisUseCase`) — validates the user, creates a `VideoAnalysis` row in
   `CREATED` state, and returns a presigned S3 upload URL (15 min expiry) built from a key of the form
   `videos/{exerciseType}/{userId}/{uuid}.mp4`.
1. Client uploads the video directly to S3 using that URL.
1. `POST /api/v1/analysis/videos/{id}/upload-complete` (`ConfirmVideoUploadUseCase`) — transitions the analysis to
   `UPLOADED` and publishes a `ConfirmVideoUploadedEvent` (event type `"VideoAnalysisUploaded"`) to RabbitMQ via
   `RabbitMQEventPublisher`.
1. The event lands in `video-analysis.exchange` (direct exchange, shared by all three pipeline segments below),
   routed by key `video-analysis.uploaded` into `video-analysis.queue`. All three services declare their own
   corner of this topology using a symmetric naming convention — `RabbitMQConfig.java` uses `EXTRACTION_*` /
   `SCORING_*` / `RESULTS_*` constants (queue, routing key, DLX, DLQ, DLQ routing key), and the two Python workers'
   `listener.py` files use the identical literal strings under the same names. **If you change any queue/exchange
   name or argument, update every side that declares it** (see the table below) — nothing declares idempotently
   relative to another service, and a mismatch causes queue redeclaration errors.
1. `pose-feature-extractor-worker/listener.py` consumes the message, downloads the video from S3 (`s3.py`), and runs
   `worker.py`'s `get_squat_bottoms_multiple_reps` to extract per-rep pose features via MediaPipe.
1. On success, the extractor publishes `{"videoAnalysisId": ..., "features": aggregate_rep_features(...)}` to
   routing key `video-analysis.extracted` (`SCORING_ROUTING_KEY`), landing in `video-analysis.scoring.queue` — this
   is what triggers the second stage. A `None` `features` result (pose detected but no valid reps) is treated as a
   failure rather than silently acked.
1. `score-analyzer-worker/listener.py` consumes that message and scores it via `SquatScorePredictor.predict(...)`.
   On success it publishes `{"videoAnalysisId", "modelVersion", "overallScore", "feedback", "rawFeatures"}` to
   routing key `video-analysis.finished` (`RESULTS_ROUTING_KEY`), landing in `video-analysis.results.queue`. Note
   there is **no `classification` field** in this payload — see the DDD note below.
1. `AnalysisResultListener` (API) consumes `video-analysis.results.queue`, derives `Classification` from the raw
   score via `Classification.fromScore(...)`, and calls `RecordAnalysisResultUseCase`, which persists an
   `AnalysisResult` and transitions the `VideoAnalysis` to `COMPLETED` via `markCompleted(...)`.
1. **Retry + DLQ, per stage.** Both Python workers share the same in-process retry shape: `MAX_RETRIES` (3) attempts
   with a blocking `time.sleep(2**(attempt-1))` between them, all within a single message delivery — never a
   RabbitMQ-level `nack`+`requeue` loop. On the final failure, the worker itself builds
   `{"analysisId", "stage", "errorMessage", "stackTrace"}` and explicitly `basic_publish`es it to its own dedicated
   DLX/DLQ (`EXTRACTION_DLX`/`EXTRACTION_DLQ` for the extractor, `SCORING_DLX`/`SCORING_DLQ` for the scorer), then
   `ack`s the original message — it does **not** rely on RabbitMQ's native dead-lettering for this path. Each
   worker's queue still declares `x-dead-letter-exchange` args pointing at its own DLX as a safety net for crashes
   outside the retry loop (e.g. the process dying mid-message), which is the one case a raw, unstructured message
   can still land in a DLQ.
1. `PipelineErrorListener` (API) consumes **both** `video-analysis.dlq` (extraction) and `video-analysis.scoring.dlq`
   (scoring) — they carry the same structured shape, so one listener handles both. It parses the raw AMQP message
   body defensively rather than relying on message-converter auto-binding: a missing/unparseable `analysisId` is
   logged and dropped (no `TB_PIPELINE_ERRORS.ANALYSIS_ID` to attach it to); a missing `stage` is defaulted from
   which queue the message arrived on. It calls `RecordPipelineErrorUseCase`, which persists a `PipelineError` and
   transitions the `VideoAnalysis` to `FAILED` via `recordFailure(...)`.
1. **Idempotency.** `VideoAnalysis.markCompleted`/`recordFailure` both guard on the current status being `UPLOADED`
   (the same `validateTransition` mechanism `confirmUpload()` uses) — a duplicate delivery of an already-terminal
   analysis throws `InvalidStatusTransitionException`, which both listeners catch and treat as a no-op (logged
   warning, message still acked) rather than a failure.

| Segment | Routing key | Main queue | DLX / DLQ | Declared by |
|---|---|---|---|---|
| Extraction | `video-analysis.uploaded` | `video-analysis.queue` | `video-analysis.dlx` / `.dlq` | API (publisher + DLQ consumer), extractor (consumer) |
| Scoring | `video-analysis.extracted` | `video-analysis.scoring.queue` | `video-analysis.scoring.dlx` / `.dlq` | extractor (publisher, defensive declare), scorer (consumer + primary), API (DLQ consumer) |
| Results | `video-analysis.finished` | `video-analysis.results.queue` | `video-analysis.results.dlx` / `.dlq` | scorer (publisher, defensive declare), API (consumer + primary) |

`score-analyzer-worker` (`SquatScorePredictor` in `inference/squat_predictor.py`) takes the aggregated feature dict
produced by the extractor worker and scores it with a pre-trained model (`models/squat_score_model.joblib`),
producing a 0–1 score, a qualitative label (`excellent`/`good`/`fair`/`poor`/`bad`, stashed in
`feedback.overall_label` for a future frontend), and rule-based per-dimension feedback (depth, back angle, tempo,
lockout, ROM, consistency). **Classification (`GOOD`/`FAIR`/`POOR`, matching the DB's `CHK_ANALYSIS_RESULTS_CLASSIFICATION`
constraint) is deliberately not computed in Python** — `Classification.fromScore(...)` (a static factory on the
domain enum) and `AnalysisResult.score(...)` (a smart constructor) own that mapping on the Java side, so the rule
that decides what counts as a "good" score has one source of truth instead of being duplicated across languages.

### Pose feature extraction (`workers/pose-feature-extractor-worker/worker.py`)

Rep detection is a hysteresis state machine over the knee-angle signal (`detect_reps_state_machine`): a rep begins
when the knee angle drops below `entry_threshold_pct` of an estimated standing baseline (85th percentile of the
whole signal) and ends when it rises back above `exit_threshold_pct`, with `min_rom`/`min_rep_frames` guards against
noise. Angles are computed per-frame from MediaPipe pose landmarks, missing frames are linearly interpolated, then
the whole signal is Savitzky-Golay smoothed before rep detection runs. Per-rep features (`extract_rep_phase_features`)
split each rep into eccentric/concentric phases and compute angle, tempo, velocity, depth, and smoothness metrics;
`aggregate_rep_features` reduces the list of per-rep feature dicts into the single averaged (+ std) feature vector
that `SquatScorePredictor` expects as input — the `FEATURE_COLS` list in `squat_predictor.py` must stay in sync with
whatever keys `aggregate_rep_features` produces.

### Frontend: feature-based structure & auth

`frontend/src` has no framework-imposed structure (plain Vite SPA), so the convention established by the sign-up
feature is: generic infrastructure lives in `src/lib/`, and everything specific to one feature lives together
under `src/features/<feature>/`.

- `src/lib/api/httpClient.ts` — the only place that calls `fetch`. A thin wrapper: same-origin relative paths
  (`/api/v1/...`, works through the Vite proxy in dev and will work through nginx in prod), `credentials: 'include'`
  so the httpOnly refresh cookie round-trips, and it normalizes `GlobalExceptionHandler`'s `{ errors: string[] }`
  response shape into a typed `ApiError` (`status` + `errors`). Feature-level API modules (e.g.
  `src/features/auth/api/authApi.ts`) call this instead of `fetch` directly — keeps HTTP/error-shape concerns out
  of components and out of every individual feature. Calls that need a session pass `{ authenticated: true }`,
  which attaches `Authorization: Bearer` (via a `getAccessToken` callback registered from `AuthProvider` through
  `configureAuth()` — `httpClient` lives outside the React tree, so this is how it reads current session state) and,
  on a `401`, refreshes the session once and retries the request once before giving up; concurrent 401s share one
  in-flight refresh call instead of racing separate ones. Public calls (`login`/`register`/`refresh` themselves)
  leave `authenticated` unset — a `401` there (e.g. a wrong password) is a normal form error, not an expired
  session, so it's never treated as a refresh trigger.
- `src/features/<feature>/` — `api/` (gateway functions per endpoint), `schemas/` (zod schemas, mirroring the
  backend DTO's bean validation so the form rejects the same inputs the API would — only where that validation
  actually exists server-side; `LoginRequestDTO` has none, so `loginSchema` only checks non-empty), `hooks/` (React
  state around an API call — loading/error/submit), `components/` (the actual form/UI), `context/` (feature-scoped
  React context, e.g. auth). See `src/features/auth/` as the reference implementation.
- **Auth state (`src/features/auth/context/AuthContext.tsx`)** — the access token lives in memory only (plain
  `useState` in a context provider), never `localStorage`/`sessionStorage`, keeping it out of reach of XSS-readable
  storage; the API pairs this with an httpOnly refresh cookie for the longer-lived session. `AuthProvider`:
  - Exposes `isInitializing` (starts `true`) alongside `accessToken`/`isAuthenticated`/`setSession`/`logout`.
    On mount it calls `POST /auth/refresh` to silently redeem any existing refresh cookie (so a hard reload of a
    protected route doesn't bounce a logged-in user to `/login`), flipping `isInitializing` to `false` once that
    settles either way.
  - Schedules a **proactive refresh** timer ~60s before the current access token's `expiresIn` elapses (jittered
    ±15s), so the reactive `httpClient` 401-retry path is rarely hit in practice. A proactive refresh that fails
    (e.g. a sibling tab already rotated the refresh token — see below) is silently skipped rather than logging the
    user out; only a failure of the *reactive* path in `httpClient` ends the session.
  - Bootstrap, the proactive timer, and `httpClient`'s reactive refresh all funnel through one single-flight
    `refreshSession` function (deduped via an in-flight-promise ref), since the backend rotates the refresh token
    on every use and concurrent calls would otherwise race each other. See `docs/adr/0002-refresh-jitter-fail-soft.md`
    for why a narrow multi-tab race is accepted instead of cross-tab coordination, and
    `docs/adr/0001-no-csrf-token-for-refresh-cookie.md` for why `/auth/refresh` has no CSRF token (relies on
    `SameSite=Strict` + same-origin instead).
- **`src/routes/ProtectedRoute.tsx`** — while `useAuth().isInitializing` is true, renders a `RouteSkeleton` (neither
  redirecting nor showing protected content until the bootstrap refresh settles); once resolved, renders `<Outlet/>`
  if authenticated, otherwise `<Navigate to="/login"/>`. **`src/routes/AuthRoute.tsx`** is the inverse guard for
  `/login`/`/register` — same `isInitializing` gate, then redirects an already-authenticated user to `/overview`
  instead of showing them the sign-in/sign-up form again.
- **Routes** (`src/main.tsx`): `/` (landing, unguarded), `/login` and `/register` (both render `AuthPage` — keyed by
  a `mode` prop, `LoginForm`/`RegisterForm` respectively — wrapped in `AuthRoute`), and `/overview` (wrapped in
  `ProtectedRoute` — the logged-in landing area a successful sign-up/sign-in redirects to; deliberately not named
  `/dashboard`, kept short since it'll eventually host more than one dashboard-shaped view).
- The project's shadcn style (`components.json`, style `radix-nova`) does **not** ship a `form` primitive (`npx
  shadcn add form` is a no-op in this registry) — forms wire `react-hook-form` directly to the generated
  `Input`/`Label` primitives with `zodResolver`, rather than the `FormField`/`FormItem` wrapper other shadcn
  projects use. See `RegisterForm.tsx`/`LoginForm.tsx` for the pattern to follow for the next form.

## Testing notes

- Unit tests for use cases (`core/usecases/**/*ImplTest.java`) instantiate the `*Impl` directly with mocked gateways
  — no Spring context involved. The same pattern covers the messaging adapters (`AnalysisResultListenerTest`,
  `PipelineErrorListenerTest`) — mocked use cases, no `@RabbitListener` container involved.
- Integration tests (`*IT.java`) extend `AbstractIntegrationTest`, which boots shared static Testcontainers
  (Postgres, LocalStack S3, RabbitMQ) once for the whole JVM and wires their connection info in via
  `@DynamicPropertySource`. This means integration tests require Docker to be available locally.
- Both Python workers have a `tests/test_listener.py` (pytest) covering the pure payload-building/decision logic
  (event and error-payload shaping, the "treat as failure" guards) with I/O (S3, the predictor, pika) mocked out —
  not the CV pipeline internals or the AMQP wire itself, which stay covered by manual end-to-end verification. Each
  worker's flat, non-package script layout needs a `conftest.py` that inserts the worker's own directory onto
  `sys.path` for `import listener` to resolve; run via `pytest` from inside each worker's directory.


## Workflow

- Always update CLAUDE.MD and README.md after new relevant features and architecture changes or something stated there changes.
- Update postman collection inside /postman folder after new endpoints creations on SPRING API.
- Run all tests after changes. Spring API Unit and integration tests and pyhton workers tests.

## Agent skills

### Issue tracker

Issues and specs live as markdown files under `.scratch/<feature-slug>/` in this repo. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`) used as-is. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout — `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.