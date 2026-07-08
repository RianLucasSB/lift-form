# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Liftform is a gym-movement analyzer. A user uploads a video of a lift (currently only squats); the video is
processed by a computer-vision pipeline to extract rep-level pose features, which are then scored for form
quality. The system is a polyglot monorepo with three parts that communicate over RabbitMQ and S3-compatible
object storage:

- `api/liftform` — Java 21 / Spring Boot 4 REST API (source of truth for users, auth, and video-analysis records)
- `workers/pose_feature_extractor_worker` — Python worker that consumes upload events, downloads the video, runs
  MediaPipe pose estimation, and computes rep/angle features
- `workers/score_analyzer_worker` — Python package (model training + inference) that turns extracted features into
  a 0–1 form score and per-dimension feedback; not yet wired into a running service/consumer

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
docker compose up            # base (rabbitmq, postgres, minio) + docker-compose.override.yml (spring_api, pose_feature_extractor_worker) — used for local dev, loaded automatically
docker compose -f docker-compose.yml -f docker-compose.prod.yml up   # prod-style compose, images pulled from ECR instead of built locally
```

`docker-compose.override.yml` is picked up automatically by `docker compose` alongside `docker-compose.yml`, which
is why local dev only needs `docker compose up`.

### Pose feature extractor worker (`workers/pose_feature_extractor_worker/`)

```bash
pip install -r requirements.txt
python listener.py     # connects to RabbitMQ and consumes video-analysis.queue
```

Requires `RABBITMQ_URL` and AWS/S3 env vars (see `.env` in that directory); against local dev stack these point at
the `minio` and `rabbitmq` docker-compose services.

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
not persisted).

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
1. The event lands in `video-analysis.exchange` (direct exchange), routed by key `video-analysis.uploaded` into
   `video-analysis.queue`. Both the API (`RabbitMQConfig`) and the Python worker (`listener.py`) independently
   declare the same exchange/queue/DLQ topology — **if you change queue/exchange names or arguments, update both
   sides**, since neither declares idempotently relative to the other and a mismatch causes queue redeclaration
   errors.
1. `pose_feature_extractor_worker/listener.py` consumes the message, downloads the video from S3 (`s3.py`), and runs
   `worker.py`'s `get_squat_bottoms_multiple_reps` to extract per-rep pose features via MediaPipe.
1. Failed messages are retried up to `MAX_RETRIES` (3) with exponential backoff via `nack`+`requeue`; once the
   dead-letter count exceeds that, the message is dropped to `video-analysis.dlq` (`video-analysis.dlx` exchange).

`score_analyzer_worker` (`SquatScorePredictor` in `inference/squat_predictor.py`) takes the aggregated feature dict
produced by the extractor worker and scores it with a pre-trained model (`models/squat_score_model.joblib`),
producing a 0–1 score, a qualitative label, and rule-based per-dimension feedback (depth, back angle, tempo,
lockout, ROM, consistency). This worker is not currently consuming from RabbitMQ or wired into docker-compose —
treat it as a library invoked by whatever will eventually be the second-stage worker, not a running service today.

### Pose feature extraction (`workers/pose_feature_extractor_worker/worker.py`)

Rep detection is a hysteresis state machine over the knee-angle signal (`detect_reps_state_machine`): a rep begins
when the knee angle drops below `entry_threshold_pct` of an estimated standing baseline (85th percentile of the
whole signal) and ends when it rises back above `exit_threshold_pct`, with `min_rom`/`min_rep_frames` guards against
noise. Angles are computed per-frame from MediaPipe pose landmarks, missing frames are linearly interpolated, then
the whole signal is Savitzky-Golay smoothed before rep detection runs. Per-rep features (`extract_rep_phase_features`)
split each rep into eccentric/concentric phases and compute angle, tempo, velocity, depth, and smoothness metrics;
`aggregate_rep_features` reduces the list of per-rep feature dicts into the single averaged (+ std) feature vector
that `SquatScorePredictor` expects as input — the `FEATURE_COLS` list in `squat_predictor.py` must stay in sync with
whatever keys `aggregate_rep_features` produces.

## Testing notes

- Unit tests for use cases (`core/usecases/**/*ImplTest.java`) instantiate the `*Impl` directly with mocked gateways
  — no Spring context involved.
- Integration tests (`*IT.java`) extend `AbstractIntegrationTest`, which boots shared static Testcontainers
  (Postgres, LocalStack S3, RabbitMQ) once for the whole JVM and wires their connection info in via
  `@DynamicPropertySource`. This means integration tests require Docker to be available locally.
