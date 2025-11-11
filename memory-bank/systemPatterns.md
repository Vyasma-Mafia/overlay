# System Patterns

This file documents recurring patterns and standards used in the project.

## Coding Patterns

### Kotlin & Spring Boot
- **Data classes**: Used extensively for entities and DTOs (e.g., `Game`, `GamePlayer`, `Fact`)
- **Extension functions**: Custom extensions for logging (`getLogger()`) and other utilities
- **Spring annotations**: 
  - `@RestController` for REST endpoints
  - `@Service` for business logic
  - `@Repository` for data access (extends `JpaRepository`)
  - `@Autowired` constructor injection (implicit in Kotlin)
  - `@ConditionalOnProperty` for conditional bean creation
- **Null safety**: Leveraging Kotlin's null safety features throughout
- **Coroutines**: Used in `EmitterService` for concurrent SSE operations

### JPA/Hibernate Patterns
- **Entity mapping**: 
  - `@Entity` and `@Table` for table mapping
  - `@Id` with `@GeneratedValue(strategy = GenerationType.UUID)` for primary keys
  - `@OneToMany` and `@ManyToOne` for relationships
  - `@Enumerated(EnumType.STRING)` for enum storage
  - `@Version` for optimistic locking
- **JSONB columns**: 
  - `@JdbcTypeCode(SqlTypes.JSON)` with `@Column(columnDefinition = "jsonb")` for PostgreSQL JSONB
  - Used for complex nested data (e.g., `voteCandidates`, `checks`, `guess`, `votedBy`, `stat`)
  - Default values: `[]` for arrays, `{}` for objects
- **Cascade operations**: `CascadeType.ALL` with `orphanRemoval = true` for dependent entities
- **Fetch strategies**: `FetchType.EAGER` for frequently accessed relationships

### Repository Pattern
- **Spring Data JPA**: All repositories extend `JpaRepository<Entity, UUID>`
- **Custom queries**: Using `@Query` annotations for complex queries
- **Method naming**: Spring Data method naming conventions for simple queries
- **Examples**: `GameRepository`, `PlayerRepository`, `FactRepository`

### API Design Patterns
- **RESTful conventions**: 
  - GET for retrieval
  - POST for creation/updates
  - DELETE for removal
- **Path variables**: Used for resource identification (e.g., `/{id}/game`)
- **Query parameters**: Used for optional filters and settings (e.g., `?value=true`)
- **JSON responses**: Jackson for serialization with Kotlin module support

### Error Handling Patterns
- **Custom exceptions**: 
  - `InvalidPasswordException` for authentication failures
  - `TournamentSettingsNotFoundException` for missing resources
- **Error tracking**: Fields in entities for tracking errors (e.g., `crawlFailureCount`, `lastCrawlError`)
- **Retry logic**: Configurable retry counts based on error types
- **Automatic recovery**: Background tasks for recovering from transient errors

## Architectural Patterns

### Layered Architecture (N-tier)
Clear separation of concerns:
- **Controller layer**: HTTP request handling, routing, response formatting
- **Service layer**: Business logic, orchestration, external service integration
- **Repository layer**: Data access abstraction
- **Entity layer**: Domain models and data mapping

### Service Layer Patterns
- **Single Responsibility**: Each service handles one domain (e.g., `PlayerService`, `PolemicaService`)
- **Dependency Injection**: Constructor injection throughout
- **Conditional Services**: Services conditionally enabled via `@ConditionalOnProperty`
- **Scheduled Tasks**: `@Scheduled` annotations for periodic operations

### Real-time Communication Pattern
- **Server-Sent Events (SSE)**: 
  - `SseEmitter` for client connections
  - `EmitterService` manages emitter lifecycle
  - Automatic cleanup of dead connections
  - Error counting and removal after threshold
- **Event-driven updates**: Services emit updates via `EmitterService`
- **Connection management**: ConcurrentHashMap for thread-safe emitter storage

### Integration Patterns
- **External API clients**: 
  - `PolemicaClient` from polemica-library
  - `GomafiaRestClient` for Gomafia API
- **Caching**: Caffeine cache for frequently accessed data
- **Scheduled crawling**: ExecutorService for background data fetching
- **Error resilience**: Different retry strategies based on error types

### Data Synchronization Patterns
- **Polling**: Scheduled tasks for periodic data updates
- **Manual triggers**: REST endpoints for force refresh
- **Incremental updates**: Only update changed entities
- **Conflict resolution**: Optimistic locking with `@Version` field

## Testing Patterns

### Unit Testing
- **Framework**: JUnit 5 with MockK for Kotlin
- **Test structure**: 
  - `PlayerServiceTest.kt` - Service layer tests
  - `PolemicaServiceErrorHandlingTest.kt` - Error handling tests
  - `PolemicaServiceGameTitleTest.kt` - Specific feature tests
- **Mocking**: MockK for mocking dependencies
- **Isolation**: Tests focus on single service/component

### Test Organization
- **Location**: `src/test/kotlin/com/stoum/overlay/`
- **Naming**: `*Test.kt` suffix
- **Reports**: HTML test reports in `build/reports/tests/`

## Configuration Patterns

### Application Configuration
- **Properties files**: `application.properties` for default values
- **Environment variables**: Override via environment or `.env` file
- **Type-safe configuration**: `@ConfigurationProperties` classes (`ApplicationConfig`, `ObjectStorageConfig`)
- **Profiles**: Spring profiles for different environments (prod, dev)

### Database Configuration
- **Flyway migrations**: Version-controlled schema changes
- **Naming convention**: `V{YYYYMMDD}__{description}.sql`
- **Migration strategy**: 
  - Baseline on migrate for existing databases
  - Clean disabled for safety
  - Transactional migrations

### Security Patterns
- **Current state**: Security disabled for development (CORS/CSRF disabled)
- **Password protection**: Overlay admin password for sensitive operations
- **Session management**: Spring Session with JDBC backend
- **Recommendation**: Add authentication/authorization before production deployment

## Frontend Patterns

### Thymeleaf Templates
- **Layout templates**: Base layout with fragments
- **Fragment reuse**: `_modal_fragments.html`, `_viewer_fragment.html`
- **Bootstrap 5**: Used for admin panel UI
- **Static resources**: Organized in `/static/` directory (CSS, fonts, icons, images)

### CSS Organization
- **Component-based**: Separate CSS files per component
  - `style-admin.css` - Admin panel styles
  - `style-panel.css` - Control panel styles
  - `style-role-selector.css` - Role selector styles
  - `style-source.css` - Overlay source styles
- **Naming**: BEM-like naming conventions
- **Responsive**: Mobile-friendly designs

## Deployment Patterns

### Containerization
- **Multi-stage Dockerfile**: Separate build and runtime stages
- **Docker Compose**: Orchestration of application, database, and monitoring
- **Volume mounts**: For persistent data
- **Network isolation**: Services communicate via Docker network

### Monitoring Patterns
- **Actuator endpoints**: Health, info, metrics
- **Prometheus integration**: Metrics exposed on separate port
- **Custom metrics**: Application-specific metrics via Micrometer
- **Logging**: Structured logging with Logstash encoder

## Data Patterns

### JSONB Usage
- **Migration from JSON strings**: Migrated from text columns with converters to native JSONB
- **Type safety**: Database-level JSON validation
- **Query support**: PostgreSQL JSON operators for queries
- **Performance**: Better performance and indexing capabilities (GIN indexes)

### Entity Relationships
- **Bidirectional relationships**: `@OneToMany` with `@ManyToOne` back-reference
- **Cascade deletion**: Dependent entities deleted with parent
- **Eager loading**: For frequently accessed relationships
- **Ordering**: `@OrderBy` for consistent ordering (e.g., players by place)
