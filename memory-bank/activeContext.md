# Active Context

This file tracks the project's current status, including recent changes, current goals, and open questions.

## Current Focus

The project is in active development and maintenance phase. Recent work has focused on:

- MafiaUniverse integration with HTML scraping
- Enhanced error handling for Polemica crawling
- Player facts feature for game overlays
- Database schema improvements (JSONB migration)
- Admin panel enhancements

## Recent Changes

### [2026-01-23] - MafiaUniverse Integration

- Added MafiaUniverse as third game source service (no API available, uses HTML scraping)
- Created nickname-to-player mapping table (`player_mafiauniverse_nickname`) since MafiaUniverse uses nicknames instead
  of numeric IDs
- Implemented `MafiaUniverseService` with game creation and crawling logic
- Created `MafiaUniverseClient` for HTTP requests and `MafiaUniverseHtmlParser` using JSoup
- Updated `PlayerPhotoService` with nickname-based photo lookup method for MafiaUniverse
- Updated `TournamentService` to support MafiaUniverse tournaments and participants in admin UI
- Added `MAFIAUNIVERSE` to `GameType` enum and `ServiceType` in `OverlayController`
- Updated `GameController` to handle MafiaUniverse photo updates
- Added `MafiaUniverseConfig` with conditional service enablement
- Error handling similar to Polemica (network errors, parsing errors, game not found)
- Migration: `V{YYYYMMDD}__add_mafiauniverse_support.sql`

### [2025-10-28] - JSONB Migration
- Migrated JSON string columns to PostgreSQL JSONB type
- Removed custom AttributeConverter classes
- Improved type safety and query capabilities
- Migration: `V20251028__jsonb_migration.sql`

### [2025-10-11] - Facts Feature Implementation
- Completed full implementation of player facts functionality
- Facts now tied to individual games instead of tournaments
- Added `isDisplayed` flag for tracking shown facts
- Automatic fact display based on game stages from Polemica
- Full admin UI for managing facts per game

### [2025-09-25] - Database Migration Fix
- Fixed migration issue with `crawlFailureCount` field
- Changed to nullable (Int?) for database compatibility
- Updated code to handle nullable values correctly

### [2025-09-24] - Enhanced Error Handling
- Implemented advanced error handling system for Polemica crawling
- Added error tracking fields to Game entity:
  - `crawlFailureCount` - Number of consecutive failures
  - `lastCrawlError` - Last error message
  - `lastFailureTime` - Timestamp of last failure
  - `crawlStopReason` - Reason for stopping crawl
- Different retry strategies based on error types:
  - HTTP 404 (game deleted) - Immediate stop
  - HTTP 401/403 (auth issues) - Stop after 5 attempts
  - Network errors - Stop after 3 attempts
  - Unknown errors - Stop after 2 attempts
- Added recovery methods for manual and automatic restart

### [2025-09-19] - UI Improvements
- Fixed alignment of "Voted by" elements in overlays
- Changed CSS `justify-content` from `center` to `flex-start`
- Added black text-shadow for better visibility of yellow sheriff numbers

### [2025-09-18] - Game Title Enhancement
- Improved game title generation in Polemica
- Added table number when multiple tables in phase
- Added "Финал" (Final) marker for phase=2
- Created helper functions: `getTablesCountInPhase()` and `generateGameTitle()`

### [2025-09-12] - Voting Visualization
- Added display of player numbers who voted for each voted player
- New field `votedBy` in `GamePlayer` model
- Visual badges showing voting information
- Color-coded by player roles

## Current State

### Application Status
- **Version**: Active development
- **Database**: PostgreSQL 16 with Flyway migrations
- **Build**: Gradle with Kotlin 2.1.10
- **Runtime**: JDK 21, Spring Boot 3.4.4

### Key Components Status
- ✅ Core overlay functionality - Operational
- ✅ Polemica integration - Active with error handling
- ✅ Gomafia integration - Active
- ✅ MafiaUniverse integration - Active with HTML scraping
- ✅ Admin panel - Functional
- ✅ Player facts - Implemented
- ✅ Photo management - Operational with S3
- ✅ SSE real-time updates - Working
- ✅ Scheduled crawling - Configurable

### Known Issues / Technical Debt
- Security configuration currently disabled (CORS/CSRF) - needs review before production
- Some admin endpoints may need authentication/authorization
- Error handling could be extended to other integrations
- Test coverage could be expanded

## Open Questions / Issues

### Functional Questions
- What are the specific requirements for overlay functionality for different game types?
- Are there plans for additional overlay features or customization options?
- Should there be user authentication/authorization for admin panel access?
- Are there requirements for audit logging of admin actions?

### Technical Questions
- What is the expected scale (concurrent games, users, tournaments)?
- Are there performance requirements or SLAs to meet?
- Should there be rate limiting on API endpoints?
- Are there plans for horizontal scaling or load balancing?

### Integration Questions
- How should synchronization conflicts be resolved between external services?
- Are there plans for additional external service integrations?
- What is the expected data consistency model between services?

### Future Enhancements
- Additional overlay customization options
- Enhanced analytics and reporting
- Mobile app or responsive improvements
- Real-time collaboration features
- Advanced tournament management features

## Next Steps

### Immediate Priorities
1. Continue monitoring and improving error handling
2. Expand test coverage for new features
3. Review and enhance security configuration
4. Document API endpoints more thoroughly

### Short-term Goals
- Improve admin panel UX
- Add more comprehensive error logging
- Optimize database queries if needed
- Enhance monitoring and alerting

### Long-term Considerations
- Security hardening for production deployment
- Performance optimization for scale
- Additional integration features
- Enhanced analytics capabilities
