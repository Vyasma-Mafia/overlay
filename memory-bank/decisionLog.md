# Decision Log

This file records architectural and implementation decisions using a list format.

## [2025-10-28] - JSONB Migration Decision

### Decision
Migrate JSON string columns to PostgreSQL JSONB type via Flyway migration without indexes at this stage.

### Rationale
- **Type safety**: Database-level JSON validation
- **Query capabilities**: Native PostgreSQL JSON operators and path queries
- **Performance**: Better performance and indexing potential (GIN indexes)
- **Reduced overhead**: Eliminate custom AttributeConverter classes
- **Future-proofing**: Better foundation for complex queries and indexing

### Implementation Details
- **Migration file**: `V20251028__jsonb_migration.sql`
- **Columns migrated**:
  - `game.vote_candidates`: `[]` default
  - `game_player.checks`: `[]` default
  - `game_player.guess`: `[]` default
  - `game_player.voted_by`: `[]` default
  - `game_player.stat`: `{}` default
- **Entity updates**: 
  - Replaced `@Convert` with `@JdbcTypeCode(SqlTypes.JSON)` and `@Column(columnDefinition = "jsonb")`
  - Removed `MapListConverter` and `MapMapConverter` classes
- **Transaction safety**: Single transaction migration, no downtime

## [2025-10-11] - Facts Feature Refactoring

### Decision
Refactor facts from tournament-level to game-level association, with `isDisplayed` flag for tracking.

### Rationale
- **Better granularity**: Facts are game-specific, not tournament-wide
- **Display control**: Track which facts have been shown to prevent duplicates
- **Stage integration**: Better integration with Polemica game stages
- **Flexibility**: Allow different facts for different games in same tournament

### Implementation Details
- **Entity changes**: 
  - `Fact.game_id` instead of `tournament_overlay_settings_id`
  - Added `isDisplayed: Boolean` field
  - `@OneToMany` relationship in `Game` entity
- **Service changes**: 
  - `PolemicaService` automatically displays facts based on game stages
  - Integration with `StageType` from Polemica library
- **Admin UI**: New `game_facts.html` page for managing facts per game
- **Repository**: New `FactRepository` with game-specific queries

## [2025-09-24] - Advanced Error Handling for Crawling

### Decision
Implement comprehensive error handling system for Polemica crawling with error type differentiation and automatic recovery.

### Rationale
- **Error differentiation**: Different error types require different handling strategies
- **Prevent infinite retries**: Counter-based stopping prevents resource waste
- **Automatic recovery**: Transient errors should recover automatically
- **Monitoring**: Detailed error tracking enables better diagnostics
- **Administrative control**: Manual recovery options for administrators

### Implementation Details
- **New fields in Game entity**:
  - `crawlFailureCount: Int?` - Consecutive failure counter
  - `lastCrawlError: String?` - Last error message
  - `lastFailureTime: LocalDateTime?` - Timestamp tracking
  - `crawlStopReason: String?` - Reason for stopping
- **Error handling strategies**:
  - HTTP 404 (game deleted): Immediate stop
  - HTTP 401/403 (auth): Stop after 5 attempts
  - Network errors: Stop after 3 attempts
  - Unknown errors: Stop after 2 attempts
- **Recovery methods**:
  - `restartGameCrawling()` - Manual restart
  - `autoRecoverStoppedGames()` - Automatic recovery for transient errors
- **Monitoring**: `getCrawlErrorStatistics()` and `getProblematicGames()` methods

## [2025-09-18] - Game Title Generation Enhancement

### Decision
Enhance game title generation in Polemica to include table numbers and phase markers.

### Rationale
- **Information clarity**: Distinguish games on different tables in multi-table phases
- **Final identification**: Clear marking of final games
- **User experience**: Better understanding of game context for viewers
- **Backward compatibility**: Maintain existing format for single-table scenarios

### Implementation Details
- **New format**:
  - Single table: `"Tournament | Game N"`
  - Multiple tables: `"Tournament | Game N | Table M"`
  - Final phase: `"Tournament | Final | Game N"` or `"Tournament | Final | Game N | Table M"`
- **Helper functions**:
  - `getTablesCountInPhase()` - Count tables in phase
  - `generateGameTitle()` - Generate formatted title
- **Location**: `PolemicaService.kt` line 154 in `createGameFromPolemica()`

## [2025-09-12] - Voting Visualization Feature

### Decision
Add visualization of which players voted for each voted player, displayed as numbered badges.

### Rationale
- **Information transparency**: Viewers can see voting patterns
- **Visual clarity**: Badge-based display is compact and clear
- **Design consistency**: Uses existing color-coding system
- **User experience**: Enhances understanding of game dynamics

### Implementation Details
- **Backend**: 
  - New field `votedBy: MutableList<Map<String, String>>?` in `GamePlayer`
  - Stored as JSONB in database
- **Frontend**:
  - HTML container `.voted-by-container` with badges `.voted-by-badge`
  - Compact design (18px height)
  - Color-coded by player roles
  - Smooth animations with staggered delays
- **Display conditions**: Only shown for players with status "voted"
- **Positioning**: Above player name, below photo

## [2025-09-03] - Initial Memory Bank Creation

### Decision
Create initial Memory Bank structure based on automatic analysis of project structure and codebase.

### Rationale
- **Context establishment**: Provide foundation for all future operations
- **Efficiency**: Automatic analysis provides quick high-level understanding
- **Completeness**: Capture technology stack, architecture, and project purpose
- **Documentation**: Centralized knowledge base for project understanding

### Implementation Details
- Analyzed file structure, dependencies, and configuration files
- Reviewed existing code patterns and architecture
- Documented key features and components
- Established baseline for ongoing updates

## [Initial] - Technology Stack Selection

### Decision
Use Spring Boot 3.4 with Kotlin, PostgreSQL, and SSE for real-time updates.

### Rationale
- **Spring Boot**: Mature framework with excellent ecosystem
- **Kotlin**: Modern language with null safety and concise syntax
- **PostgreSQL**: Robust relational database with JSONB support
- **SSE**: Simple and effective for server-to-client push updates
- **Thymeleaf**: Server-side templating for admin panel

### Implementation Details
- Spring Boot 3.4.4 with Kotlin 2.1.10
- PostgreSQL 16 with Flyway migrations
- Server-Sent Events via Spring Web MVC
- Thymeleaf for HTML generation
- S3-compatible storage for media files

## [Initial] - Architecture: Layered N-tier

### Decision
Adopt layered architecture with clear separation: Controller → Service → Repository → Entity.

### Rationale
- **Separation of concerns**: Clear boundaries between layers
- **Testability**: Easy to mock dependencies
- **Maintainability**: Changes isolated to specific layers
- **Scalability**: Easy to add new features without affecting other layers
- **Standard pattern**: Well-understood architecture pattern

### Implementation Details
- Controllers handle HTTP and routing
- Services contain business logic
- Repositories abstract data access
- Entities represent domain models
- Spring dependency injection throughout

## [Initial] - Real-time Communication: SSE

### Decision
Use Server-Sent Events (SSE) instead of WebSockets for real-time updates.

### Rationale
- **Simplicity**: SSE is simpler than WebSockets (one-way communication sufficient)
- **HTTP-based**: Works through standard HTTP, easier with proxies/firewalls
- **Automatic reconnection**: Built-in browser reconnection support
- **Lower overhead**: Less protocol overhead than WebSockets
- **Sufficient for use case**: One-way server-to-client updates are all that's needed

### Implementation Details
- `SseEmitter` from Spring Web MVC
- `EmitterService` manages emitter lifecycle
- Automatic cleanup of dead connections
- Error counting and threshold-based removal
- ConcurrentHashMap for thread-safe storage

## [Initial] - Database: PostgreSQL with JSONB

### Decision
Use PostgreSQL with JSONB columns for complex nested data structures.

### Rationale
- **JSONB benefits**: Type safety, query capabilities, indexing potential
- **Flexibility**: Store variable structures without schema changes
- **Performance**: Better than text JSON with converters
- **PostgreSQL features**: Rich JSON operators and functions
- **Future-proofing**: Can add GIN indexes for performance if needed

### Implementation Details
- JSONB columns for: `voteCandidates`, `checks`, `guess`, `votedBy`, `stat`
- Default values: `[]` for arrays, `{}` for objects
- `@JdbcTypeCode(SqlTypes.JSON)` annotation
- Column definition: `columnDefinition = "jsonb"`

## [Initial] - External Storage: S3-compatible

### Decision
Use S3-compatible object storage (Yandex Object Storage) for player photos.

### Rationale
- **Scalability**: Object storage scales better than file system
- **Separation**: Media storage separate from application
- **CDN integration**: Easy to integrate with CDN if needed
- **Cost-effective**: Pay for what you use
- **Flexibility**: Can switch providers if needed

### Implementation Details
- AWS SDK for Kotlin
- S3 Transfer Manager for efficient uploads
- Configuration via `ObjectStorageConfig`
- Environment variables for credentials
- Bucket-based organization

## [Initial] - Monitoring: Prometheus + Actuator

### Decision
Use Spring Boot Actuator with Prometheus for monitoring and metrics.

### Rationale
- **Standard solution**: Actuator is Spring Boot standard
- **Prometheus**: Industry-standard metrics format
- **Integration**: Easy integration with monitoring stacks
- **Health checks**: Built-in health endpoint
- **Custom metrics**: Can add application-specific metrics

### Implementation Details
- Actuator on separate port (8081)
- Prometheus metrics endpoint
- Micrometer for metrics collection
- Separate Prometheus service in Docker Compose
- Configuration via `prometheus.yml`

## [2026-01-23] - MafiaUniverse Integration with Nickname Mapping

### Decision

Add MafiaUniverse as a third game source service using HTML scraping, with a nickname-to-player mapping table since
MafiaUniverse identifies players by nicknames instead of numeric IDs.

### Rationale

- **No API available**: MafiaUniverse doesn't provide an API, requiring HTML scraping
- **Nickname-based identification**: Unlike Polemica/Gomafia which use numeric IDs, MafiaUniverse uses string nicknames
- **Mapping solution**: Create a separate mapping table to link MafiaUniverse nicknames to internal Player UUIDs
- **Consistency**: Maintains the same service pattern as other integrations while handling the different identification
  scheme
- **Flexibility**: Mapping table allows handling nickname changes and multiple nicknames per player

### Implementation Details

- **New entity**: `PlayerMafiaUniverseNickname` with `playerId` (UUID) and `nickname` (String)
- **Migration**: `V{YYYYMMDD}__add_mafiauniverse_support.sql` creates mapping table with unique constraint on nickname
- **HTML scraping**:
    - `MafiaUniverseClient` using Spring `RestTemplate` for HTTP requests
    - `MafiaUniverseHtmlParser` using JSoup library for parsing HTML
    - Parses tournaments list, games list, and game details pages
- **Service layer**:
    - `MafiaUniverseService` similar to `PolemicaService` structure
    - `PlayerService.findOrCreatePlayerByMafiaUniverseNickname()` for player lookup/creation
    - `PlayerPhotoService` overloaded method for nickname-based photo lookup
- **Configuration**:
    - `MafiaUniverseConfig` with `@ConfigurationProperties` (registered via `@EnableConfigurationProperties`)
    - Conditional service enablement via `@ConditionalOnProperty("app.mafiauniverse.enable")`
- **UI integration**:
    - `TournamentService` updated to support MafiaUniverse tournaments and participants
    - Admin panel dropdown includes MAFIAUNIVERSE option (excludes CUSTOM)
- **Error handling**: Similar to Polemica with different retry strategies for network/parsing errors
- **Dependencies**: Added JSoup library (`org.jsoup:jsoup:1.17.2`) to `build.gradle`
