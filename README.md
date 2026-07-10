# Liftform

Liftform is a gym-movement analyzer. A user uploads a video of a lift (currently only **squats**); the video is
processed by a computer-vision pipeline to extract rep-level pose features, which are then scored for form quality
and turned into actionable feedback.

The system is a polyglot monorepo made of independent services that communicate over **RabbitMQ** (events) and
**S3-compatible object storage** (video files), so each part can be deployed, scaled, and iterated on independently.

## System Overall Architecture

![alt text](image.png)

## Services

### `api/liftform` — REST API

Java 21 / Spring Boot 4. The source of truth for users, auth, and video-analysis records. Owns the Postgres database
and is the only service that talks to clients directly.

- Issues presigned S3 upload URLs so videos go straight from the client to object storage (never through the API).
- Tracks the lifecycle of a video analysis (`CREATED → UPLOADED → PROCESSING → COMPLETED`, or `FAILED`/`EXPIRED`).
- Publishes a `VideoAnalysisUploaded` event to RabbitMQ once a client confirms an upload, which is what kicks off
  processing on the worker side.
- Stateless JWT auth, with refresh tokens stored hashed in Postgres.
- Built with hexagonal / Clean Architecture — see [Architecture](#architecture) below.

### `workers/pose_feature_extractor_worker` — pose extraction worker

Python. Consumes `VideoAnalysisUploaded` events off RabbitMQ, downloads the video from S3, and runs MediaPipe pose
estimation over it to extract rep-level features (angles, tempo, depth, smoothness, etc. per rep).

- Rep detection is a hysteresis state machine over the knee-angle signal, with the raw signal smoothed
  (Savitzky-Golay) before detection to reduce landmark noise.
- Failed messages are retried up to 3 times with exponential backoff (`nack` + requeue) before being routed to a
  dead-letter queue, so a bad video or a transient MediaPipe failure doesn't block the queue.
- This is the service expected to carry the most load (video download + CV inference per message), so it's built to
  be horizontally scaled — see [Scaling](#scaling) below.

### `workers/score_analyzer_worker` — score analyzer (not yet wired into a running service)

Python package containing model training and inference code. Takes the aggregated feature dict produced by the
extractor worker and scores it with a pre-trained model, producing a 0–1 form score, a qualitative label, and
rule-based per-dimension feedback (depth, back angle, tempo, lockout, range of motion, consistency).

Today this is a library, not a consumer — there is no listener wired up to invoke it automatically. The intent is
for it to become the second stage of the pipeline (consuming a "features extracted" event and publishing a
"score computed" event), but that integration hasn't been built yet.

## API Architecture

The API follows **Clean Architecture** with some **DDD** principles, structured so that the domain and business
rules never depend on frameworks or infrastructure:

- **`core/domain`** — framework-free domain model. `VideoAnalysis` is a rich domain object (not an anemic
  DTO/entity): it's created via named factory methods (`initialize`, `reconstitute`) and its own state transitions
  are validated internally, rather than left to callers to get right.
- **`core/gateway`** — interfaces (ports) the domain/use-case layer depends on for persistence, storage, eventing,
  and security. No Spring, JPA, or AMQP types leak into this layer.
- **`core/usecases`** — one use case per folder, each a plain class with constructor-injected gateways and zero
  Spring annotations, which keeps business logic unit-testable with plain mocks instead of a Spring context.
- **`infraestructure/adapter`** — concrete implementations of the gateway interfaces (JPA repositories, RabbitMQ
  publisher, S3 storage, JWT/password hashing) plus a decorator layer that wraps use cases with `@Transactional`
  boundaries, so transaction management stays out of the framework-free use case classes.
- **`presentation`** — controllers, request/response DTOs, mappers, and centralized exception-to-HTTP translation.

The payoff of this split: business rules (how a video analysis moves through its lifecycle, what counts as a valid
transition) are testable and reviewable without spinning up Spring, a database, or a broker, and infrastructure
(swap Postgres, swap RabbitMQ, swap S3 for something else) can change without touching domain logic.

## Key design decisions

**Why RabbitMQ.** Chosen partly to learn it, but it's also a deliberate fit for this pipeline: we want direct
control over queue topology, retry/backoff behavior, and dead-lettering, rather than relying on a managed
pub/sub abstraction that hides those knobs. The API and the extractor worker each declare the same
exchange/queue/DLQ topology independently (they don't declare idempotently against each other), so a topology
change has to be made on both sides at once.

**Why separate services instead of a single deployable.** The pose extraction worker is expected to be the
bottleneck — it does a video download plus CV inference per message, while the API and the (future) score analyzer
are comparatively cheap. Splitting them lets the extractor worker scale independently: today that means running
multiple worker containers side by side; later it can mean a proper autoscaled worker fleet, without needing to
scale the API or database alongside it.

**Why direct-to-S3 upload.** The API issues a short-lived presigned URL and never proxies the video bytes itself,
so large video uploads don't tie up API request threads or bandwidth.

**Why Clean Architecture / DDD for the API.** With domain logic and use cases decoupled from Spring/JPA/AMQP,
the video-analysis state machine and business rules can be unit tested with plain mocks, and infrastructure choices
(database, broker, object storage) stay swappable behind gateway interfaces instead of leaking through the codebase.

## AWS services

The API and worker are cloud-agnostic in interface (they talk to S3-compatible storage and use Spring's AWS Secrets
Manager config import), but the intended deployment target is AWS:

- **S3** — object storage for uploaded lift videos. Locally this is swapped for MinIO (S3-compatible) so the whole
  stack runs without any AWS account.
- **Secrets Manager** — the API's `prod` Spring profile imports its configuration directly from Secrets Manager
  (`spring.config.import: aws-secretsmanager:/liftform/prod`) instead of reading plaintext env vars, so database
  credentials, the JWT secret, and other sensitive config never live in a file or image.
- **EC2** — deployment target for the running containers (API + worker(s)), via Docker images pulled from ECR
  (see `docker-compose.prod.yml`).

## Scaling

The services are split so each can scale independently as their workload profiles diverge — the pose extractor is
expected to need the most capacity since it's doing video download + CV inference per message, while the API is
comparatively lightweight.

Today, scaling is manual and container-based: nothing more than running multiple containers of
`pose_feature_extractor_worker` (all consuming from the same RabbitMQ queue, so messages are load-balanced across
them automatically), on the same EC2 instance for better pricing while load is still low. The near-term evolution
is to put the API (and eventually the workers) behind a load balancer (ELB/ALB) and scale out across multiple
instances instead of packing containers onto one box.

## Running locally

### Prerequisites

- Docker + Docker Compose
- JDK 21 (only needed if running the API outside Docker)
- Python 3.11+ (only needed if running a worker outside Docker)

### Full stack via Docker Compose

From the repo root:

```bash
docker compose up
```

This starts, from `docker-compose.yml` (base infra) plus `docker-compose.override.yml` (app services, picked up
automatically by `docker compose`):

| Service                        | Purpose                                            | Port(s)            |
|---------------------------------|-----------------------------------------------------|---------------------|
| `postgres`                      | API's database                                     | `5432`              |
| `rabbitmq`                      | Event broker (management UI included)              | `5672`, `15672`     |
| `minio` / `minio-setup`         | S3-compatible object storage; bucket auto-created  | `9000`, `9001`      |
| `spring_api`                    | The REST API                                       | `8080`              |
| `pose_feature_extractor_worker` | Consumes upload events, runs pose extraction       | —                   |

RabbitMQ management UI: http://localhost:15672 (`guest`/`guest`). MinIO console: http://localhost:9001
(`minioadmin`/`minioadmin`).

To run against images pulled from ECR instead of building locally (closer to the prod deployment shape):

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up
```

### Running the API standalone

```bash
cd api/liftform
./mvnw spring-boot:run -Plocal
```

Requires `postgres` and `rabbitmq` to be reachable (e.g. via `docker compose up postgres rabbitmq minio minio-setup`).
The `local` Spring profile fills in sane defaults for DB/RabbitMQ/JWT/MinIO connection details — see
`src/main/resources/application-local.yml`.

Useful test commands:

```bash
./mvnw test                                        # unit tests only
./mvnw failsafe:integration-test failsafe:verify    # integration tests (Testcontainers: needs Docker)
./mvnw test -Dtest=CreateAnalysisUseCaseImplTest    # single unit test class
```

### Running the pose extractor worker standalone

```bash
cd workers/pose_feature_extractor_worker
pip install -r requirements.txt
python listener.py
```

Requires `RABBITMQ_URL` and AWS/S3 env vars (see `.env` in that directory) — against the local dev stack these point
at the `minio` and `rabbitmq` docker-compose services.

### `score_analyzer_worker`

Not runnable as a service yet — it's a library today (see [above](#workersscore_analyzer_worker--score-analyzer-not-yet-wired-into-a-running-service)).
It can be exercised directly via its `inference`/`train` modules under `workers/score_analyzer_worker/`.

## Repository layout

```
api/liftform/                      # Spring Boot REST API
workers/pose_feature_extractor_worker/  # RabbitMQ consumer + MediaPipe pose extraction
workers/score_analyzer_worker/     # Feature scoring model + inference (library, not yet a service)
docker-compose.yml                 # Base infra: postgres, rabbitmq, minio
docker-compose.override.yml        # App services for local dev (auto-loaded alongside docker-compose.yml)
docker-compose.prod.yml            # Prod-style compose, images from ECR
```

## CI

`.github/workflows/ci.yml` triggers only on changes under `api/**`. It runs `./mvnw test` on every push/PR to
`main`, and `./mvnw failsafe:integration-test failsafe:verify` only on pushes to `main`, after unit tests pass.
