# Overlay

English documentation. Russian version: [README-RU.md](README-RU.md)

Overlay is a Kotlin/Spring Boot application that renders real-time overlays for Mafia tournaments, provides an admin panel for tournaments/players/media, and integrates with external sources (Polemica and Gomafia). Frontend pages are rendered with Thymeleaf, realtime updates are delivered via Server‑Sent Events (SSE), data is stored in PostgreSQL with Flyway migrations, and player photos are stored in an S3-compatible object storage.

## Features

- Real-time overlays for live streams (SSE-driven)
- Admin panel for tournaments, players, photos, overlay settings
- Integration with Polemica and Gomafia
- Role selector workflow, control panel
- Facts about players (per-game), with admin UI and timed display
- Prometheus metrics via Spring Boot Actuator
- Docker/Docker Compose support

## Tech stack

- Kotlin + Spring Boot 3.4 (Web, Thymeleaf, Data JPA/JDBC, Validation, Actuator)
- PostgreSQL 16 + Flyway
- SSE for realtime transport
- S3-compatible object storage (e.g., Yandex Object Storage) for photos
- Prometheus
- Build with Gradle; JDK 21

## Repository structure (high-level)

- Entry point: [OverlayApplication.kt](src/main/kotlin/com/stoum/overlay/OverlayApplication.kt)
- Controllers: overlays/UI/API in [OverlayController.kt](src/main/kotlin/com/stoum/overlay/controller/OverlayController.kt), SSE in [SseController.kt](src/main/kotlin/com/stoum/overlay/controller/SseController.kt), game state in [GameController.kt](src/main/kotlin/com/stoum/overlay/controller/GameController.kt), admin in [PhotoAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/PhotoAdminController.kt) and [GameFactsAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/GameFactsAdminController.kt)
- Core entities: [Game.kt](src/main/kotlin/com/stoum/overlay/entity/Game.kt), [overlay/GamePlayer.kt](src/main/kotlin/com/stoum/overlay/entity/overlay/GamePlayer.kt), [Fact.kt](src/main/kotlin/com/stoum/overlay/entity/Fact.kt)
- Repositories: e.g. [GameRepository.kt](src/main/kotlin/com/stoum/overlay/repository/GameRepository.kt)
- Services: SSE emitter [EmitterService.kt](src/main/kotlin/com/stoum/overlay/service/EmitterService.kt), overlay settings [TournamentOverlayService.kt](src/main/kotlin/com/stoum/overlay/service/TournamentOverlayService.kt)
- Config: [ApplicationConfig.kt](src/main/kotlin/com/stoum/overlay/config/ApplicationConfig.kt), [ObjectStorageConfig.kt](src/main/kotlin/com/stoum/overlay/config/ObjectStorageConfig.kt), [JacksonConfiguration.kt](src/main/kotlin/com/stoum/overlay/config/JacksonConfiguration.kt), [SecurityConfig.kt](src/main/kotlin/com/stoum/overlay/config/SecurityConfig.kt)
- DB migrations: [src/main/resources/db/migration](src/main/resources/db/migration)
- Docker/Compose: [Dockerfile](Dockerfile), [docker-compose.yml](docker-compose.yml)

## Requirements

- JDK 21
- Docker and Docker Compose (recommended) or local PostgreSQL 16
- Access keys for S3-compatible storage

## Quick start with Docker Compose

1) Create .env in the repository root (see .env.example below).
2) Run:

```bash
docker compose up -d
```

3) Services:
- App: http://localhost:8090
- Postgres: host 5435 -> container 5432
- Prometheus: http://localhost:9091

Compose config: [docker-compose.yml](docker-compose.yml). Prometheus config: [prometheus.yml](prometheus.yml).

## Local development (without containers)

1) Start PostgreSQL and create database "overlay".
2) Export environment variables (at minimum):

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/overlay
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
export OVERLAY_ADMIN_PASSWORD=change-me
export S3_KEY_ID=...
export S3_SECRET_ACCESS_KEY=...
```

3) Run the app:

```bash
./gradlew bootRun
```

Or build a jar and run:

```bash
./gradlew build
java -jar build/libs/app.jar
```

Dockerfile uses a two-stage build: [Dockerfile](Dockerfile).

## Configuration

Main settings live in [application.properties](src/main/resources/application.properties). Key variables (env or .env):

- DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD
- OVERLAY_ADMIN_PASSWORD (used by overlay settings admin service)
- S3_KEY_ID, S3_SECRET_ACCESS_KEY, s3.region, s3.endpoint, s3.bucket.name
- app.polemicaEnable, app.crawlScheduler.enable, app.crawlScheduler.interval
- management.* (Actuator/Prometheus on port 8081)

Example .env.example:

```dotenv
# Database
DB_USER=postgres
DB_PASSWORD=postgres

# Application database connection (container connects to service overlay-db)
DATABASE_URL=jdbc:postgresql://overlay-db:5432/overlay
DATABASE_USER=${DB_USER}
DATABASE_PASSWORD=${DB_PASSWORD}

# Overlay admin password (for enabling/disabling overlays per tournament)
OVERLAY_ADMIN_PASSWORD=change-me

# S3 (Yandex Object Storage)
S3_KEY_ID=your-key-id
S3_SECRET_ACCESS_KEY=your-secret

# Profiles
SPRING_PROFILES_ACTIVE=prod
```

## UI routes

- Overlay page:
  /{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/overlay
- Control panel:
  /{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/control
- Role selector:
  /{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/roleselector

where service ∈ {polemica, gomafia}. See [OverlayController.kt](src/main/kotlin/com/stoum/overlay/controller/OverlayController.kt).

## Realtime (SSE)

- GET /{id}/gameinfo — overlay stream
- GET /{id}/controlinfo — control stream
- GET /{id}/roleselectorinfo — role selector stream

Implemented by [SseController.kt](src/main/kotlin/com/stoum/overlay/controller/SseController.kt) and [EmitterService.kt](src/main/kotlin/com/stoum/overlay/service/EmitterService.kt).

## REST API highlights

- POST /{id}/game — push GameInfo snapshot to clients
- POST /{id}/roles — set roles in bulk (place->role)
- POST /{id}/status — set statuses in bulk
- POST /{id}/setSpeaker?playerNum=...
- POST /{id}/visibleOverlay|visibleRoles|visibleScores?value=Boolean
- POST /{id}/started|text|delay|autoNextGame
- POST /{id}/resetStatuses, POST /{id}/resetRoles
- POST /{id}/setPlayerChecks, POST /{id}/setPlayerGuesses

Admin endpoints:
- Photos/players/tournaments under /admin/photos (see [PhotoAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/PhotoAdminController.kt))
- Game facts under /admin/games (see [GameFactsAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/GameFactsAdminController.kt))
- Tournament games/search (see [AdminController.kt](src/main/kotlin/com/stoum/overlay/controller/AdminController.kt))
- Force Polemica recrawl (if enabled): POST /polemica/_force_recheck (see [PolemicaController.kt](src/main/kotlin/com/stoum/overlay/controller/PolemicaController.kt))

## Swagger UI

Springdoc OpenAPI UI is included. Default URL:

http://localhost:8090/swagger-ui/index.html

Note: Not all endpoints may be described with annotations; UI will reflect available mappings.

## Database and migrations

Flyway is enabled; migrations are in [src/main/resources/db/migration](src/main/resources/db/migration).

## Monitoring

- Actuator runs on port 8081 with Prometheus metrics exposed (see [application.properties](src/main/resources/application.properties)).
- Example Prometheus config: [prometheus.yml](prometheus.yml)

## Security

Current [SecurityConfig.kt](src/main/kotlin/com/stoum/overlay/config/SecurityConfig.kt) disables Basic/CORS/CSRF; deploy behind a trusted proxy or add authentication/authorization before exposing admin endpoints to the public internet. Overlay enable/disable is password-protected via the overlay admin service.

## Build and test

```bash
./gradlew build
./gradlew test
```

## License

Apache-2.0. See [LICENSE.md](LICENSE.md).