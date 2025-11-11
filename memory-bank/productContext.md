# Product Context

This file provides a high-level overview of the project and the expected product that will be created. This file is intended to be updated as the project evolves, and should be used to inform all other modes of the project's goals and context.

## Project Goal

Development and maintenance of the "Overlay" web application for competitive Mafia tournaments. The application provides real-time overlays for game streams, an admin panel for managing tournaments, players, and content, as well as integration with external services (gomafia.pro, polemica.app).

## Key Features

### Real-time Overlays for Live Streams
- **SSE-driven updates**: Server-Sent Events for real-time data delivery
- **Game information display**: Table composition, roles, voting status, player states
- **Player voting visualization**: Shows which players voted for each voted player
- **Role selector interface**: Interactive role assignment workflow
- **Control panel**: Manual game state management for streamers
- **Player facts**: Timed display of interesting facts about players during games

### Administrative Panel
- **Tournament management**: Create, configure, and manage tournaments
- **Player profiles**: Manage player information including photos
- **Game management**: View and manage games, filter by tournament/phase/table
- **Player search**: Search and view player information across tournaments
- **Photo management**: Upload and manage player photos stored in S3-compatible storage
- **Facts management**: Create and manage player facts per game with stage-based display
- **Overlay settings**: Enable/disable overlays per tournament with password protection

### External Integrations
- **Gomafia.pro integration**: Synchronize tournament, player, and game data
- **Polemica.app integration**: Fetch game data from Polemica service with automatic crawling
- **Error handling**: Advanced error handling for crawling with automatic recovery
- **Scheduled crawling**: Configurable interval-based automatic data updates

### Technical Features
- **Real-time updates**: Server-Sent Events (SSE) for instant client updates
- **Database migrations**: Flyway for version-controlled schema changes
- **Monitoring**: Prometheus metrics via Spring Boot Actuator
- **Containerization**: Docker and Docker Compose support
- **RESTful API**: Comprehensive REST API for all operations
- **Swagger UI**: API documentation via Springdoc OpenAPI

## Overall Architecture

### Technology Stack
- **Backend**: Spring Boot 3.4.4 application written in Kotlin
- **Language**: Kotlin 2.1.10 with JDK 21
- **Build tool**: Gradle
- **Database**: PostgreSQL 16 with Flyway migrations
- **ORM**: Spring Data JPA/Hibernate
- **Frontend**: Thymeleaf templating engine for HTML generation
- **Real-time**: Server-Sent Events (SSE) for push updates
- **Storage**: S3-compatible object storage (Yandex Object Storage) for player photos
- **Monitoring**: Prometheus metrics exposed via Actuator
- **API Documentation**: Springdoc OpenAPI (Swagger UI)

### Application Structure
- **Entry point**: `OverlayApplication.kt` - Main Spring Boot application class
- **Controllers**: 
  - `OverlayController.kt` - Overlay page rendering
  - `SseController.kt` - SSE endpoints for real-time updates
  - `GameController.kt` - Game state management API
  - `AdminController.kt` - Admin panel for tournaments and players
  - `PhotoAdminController.kt` - Photo management
  - `GameFactsAdminController.kt` - Facts management
  - `GameListAdminController.kt` - Game listing and filtering
  - `PolemicaController.kt` - Polemica integration endpoints
  - `RoleSelectorController.kt` - Role selector interface
- **Entities**: 
  - `Game.kt` - Core game entity with players and facts
  - `GamePlayer.kt` - Player information within a game
  - `Player.kt` - Player master data
  - `Fact.kt` - Player facts displayed during games
  - `TournamentOverlaySettings.kt` - Tournament-specific overlay settings
- **Services**:
  - `EmitterService.kt` - SSE emitter management
  - `PolemicaService.kt` - Polemica integration and crawling
  - `GomafiaService.kt` - Gomafia integration
  - `PlayerService.kt` - Player business logic
  - `TournamentOverlayService.kt` - Overlay settings management
- **Repositories**: Spring Data JPA repositories for all entities
- **Configuration**: 
  - `ApplicationConfig.kt` - Application properties
  - `ObjectStorageConfig.kt` - S3 configuration
  - `SecurityConfig.kt` - Spring Security configuration
  - `JacksonConfiguration.kt` - JSON serialization

### Database Schema
- **Player**: Core player information with external IDs (Polemica, Gomafia)
- **PlayerPhoto**: Player photos with tournament-specific associations
- **Game**: Game instances with tournament, phase, table, and game numbers
- **GamePlayer**: Player state within a game (role, status, votes, etc.)
- **Fact**: Player facts tied to specific games with stage-based display
- **TournamentOverlaySettings**: Tournament-level overlay configuration
- **TournamentUsageLog**: Usage statistics for tournaments
- **GameUsageLog**: Usage statistics for games

### Deployment
- **Docker**: Multi-stage Dockerfile for containerized deployment
- **Docker Compose**: Complete stack including PostgreSQL and Prometheus
- **Environment variables**: Configuration via environment variables or .env file
- **Ports**: 
  - Application: 8090
  - Actuator: 8081
  - PostgreSQL: 5435 (host) -> 5432 (container)
  - Prometheus: 9091

## Key Routes

### Overlay Routes
- `/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/overlay` - Overlay page
- `/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/control` - Control panel
- `/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/roleselector` - Role selector

Where `service` ∈ {polemica, gomafia}

### SSE Endpoints
- `GET /{id}/gameinfo` - Overlay stream
- `GET /{id}/controlinfo` - Control stream
- `GET /{id}/roleselectorinfo` - Role selector stream

### REST API Highlights
- `POST /{id}/game` - Push GameInfo snapshot
- `POST /{id}/roles` - Set roles in bulk
- `POST /{id}/status` - Set statuses in bulk
- `POST /{id}/setSpeaker?playerNum=...` - Set speaker
- `POST /{id}/visibleOverlay|visibleRoles|visibleScores?value=Boolean` - Toggle visibility
- `POST /{id}/started|text|delay|autoNextGame` - Update game settings
- `POST /{id}/resetStatuses`, `POST /{id}/resetRoles` - Reset operations
- `POST /{id}/setPlayerChecks`, `POST /{id}/setPlayerGuesses` - Set player data

### Admin Endpoints
- `/admin/photos/*` - Photo management
- `/admin/games/*` - Game facts management
- `/admin/tournaments/*` - Tournament management
- `/admin/players/*` - Player search and management
- `POST /polemica/_force_recheck` - Force Polemica recrawl

## Configuration

### Environment Variables
- `DATABASE_URL` - PostgreSQL connection URL
- `DATABASE_USER` - Database username
- `DATABASE_PASSWORD` - Database password
- `OVERLAY_ADMIN_PASSWORD` - Password for overlay enable/disable
- `S3_KEY_ID` - S3 access key ID
- `S3_SECRET_ACCESS_KEY` - S3 secret access key
- `SPRING_PROFILES_ACTIVE` - Spring profile (prod, dev, etc.)

### Application Properties
- `app.polemicaEnable` - Enable Polemica integration
- `app.crawlScheduler.enable` - Enable scheduled crawling
- `app.crawlScheduler.interval` - Crawling interval (default: 10s)
- `s3.region` - S3 region (default: ru-central1)
- `s3.endpoint` - S3 endpoint URL
- `s3.bucket.name` - S3 bucket name for photos
