package com.stoum.overlay.service.mafiauniverse

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.PlayerPhotoService
import com.stoum.overlay.service.PlayerService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(value = ["app.mafiauniverse.enable"], havingValue = "true", matchIfMissing = false)
class MafiaUniverseService(
    val mafiaUniverseClient: MafiaUniverseClient,
    val htmlParser: MafiaUniverseHtmlParser,
    val gameRepository: GameRepository,
    val emitterService: EmitterService,
    val playerService: PlayerService,
    val photoService: PlayerPhotoService
) {
    private val taskExecutorService = Executors.newScheduledThreadPool(4)

    /**
     * Gets or tries to create a game from MafiaUniverse
     */
    fun getOrTryCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        getLogger().info("Getting MafiaUniverse game tournamentId: $tournamentId, gameNum: $gameNum, tableNum: $tableNum, phase: $phase")

        var game = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
            tournamentId,
            gameNum,
            tableNum,
            phase,
            GameType.MAFIAUNIVERSE
        )

        if (game == null) {
            game = tryCreateGame(tournamentId, gameNum, tableNum, phase)
            taskExecutorService.submit { initTournament(tournamentId) }
        }

        if (game != null) {
            if (game.manuallyStarted != false) {
                game.started = true
            }
            gameRepository.save(game)
        }

        return game
    }

    /**
     * Initializes tournament by fetching all games and creating them in the database
     */
    fun initTournament(tournamentId: Int) {
        try {
            val gamesListHtml = mafiaUniverseClient.getGamesListPage(tournamentId)
            val games = htmlParser.parseGamesList(gamesListHtml)

            games.forEach { gameInfo ->
                val existingGame = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
                    tournamentId,
                    gameInfo.tourNumber, // Using tour number as gameNum
                    gameInfo.tableNumber,
                    1, // Default phase to 1 for MafiaUniverse
                    GameType.MAFIAUNIVERSE
                )

                if (existingGame == null) {
                    createGameFromMafiaUniverse(
                        tournamentId,
                        gameInfo.gameId,
                        gameInfo.tourNumber,
                        gameInfo.tableNumber
                    )
                }
            }
        } catch (e: Exception) {
            getLogger().error("Error initializing tournament $tournamentId: ${e.message}", e)
        }
    }

    /**
     * Tries to create a game by fetching game details from MafiaUniverse
     */
    private fun tryCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        try {
            // First, get games list to find the game ID
            val gamesListHtml = mafiaUniverseClient.getGamesListPage(tournamentId)
            val games = htmlParser.parseGamesList(gamesListHtml)

            val matchingGame = games.find {
                it.tourNumber == gameNum && it.tableNumber == tableNum
            }

            if (matchingGame != null) {
                return createGameFromMafiaUniverse(tournamentId, matchingGame.gameId, gameNum, tableNum)
            }
        } catch (e: Exception) {
            getLogger().warn("Error trying to create game: ${e.message}", e)
        }

        return null
    }

    /**
     * Creates a game entity from MafiaUniverse game details
     */
    private fun createGameFromMafiaUniverse(
        tournamentId: Int,
        gameId: Int,
        gameNum: Int,
        tableNum: Int
    ): Game {
        val gameDetailsHtml = mafiaUniverseClient.getGameDetailsPage(gameId)
        val parsedDetails = htmlParser.parseGameDetails(gameDetailsHtml)

        val players = parsedDetails.players.map { parsedPlayer ->
            val playerEntity = playerService.findOrCreatePlayerByMafiaUniverseNickname(parsedPlayer.nickname)
            val effectiveNickname = playerService.getEffectiveNickname(playerEntity)
            val playerPhotoUrl = photoService.getPlayerPhotoUrlForPlayerCompetitionRole(
                nickname = parsedPlayer.nickname,
                tournamentType = GameType.MAFIAUNIVERSE,
                tournamentId = tournamentId.toLong(),
                role = parsedPlayer.role
            )

            GamePlayer(
                id = null,
                nickname = effectiveNickname,
                place = parsedPlayer.position,
                photoUrl = playerPhotoUrl,
                role = parsedPlayer.role,
                checks = arrayListOf(),
                guess = arrayListOf(),
                stat = mutableMapOf(),
                customPhoto = false,
                sourcePlayerId = null // MafiaUniverse uses nicknames, not numeric IDs
            )
        }.toMutableList()

        val game = Game(
            id = null,
            type = GameType.MAFIAUNIVERSE,
            tournamentId = tournamentId,
            gameNum = gameNum,
            tableNum = tableNum,
            phase = 1, // Default phase for MafiaUniverse
            players = players,
            started = true,
            visibleOverlay = true,
            visibleRoles = parsedDetails.players.any { it.role != null },
            text = generateGameTitle(parsedDetails.tournamentName, gameNum, tableNum),
            result = parsedDetails.winner
        )

        // Ensure bidirectional association for cascading persist
        players.forEach { it.game = game }

        return gameRepository.save(game)
    }

    /**
     * Generates game title in format: "Tournament Name | Игра N | Стол M"
     */
    private fun generateGameTitle(tournamentName: String, gameNum: Int, tableNum: Int): String {
        return "$tournamentName | Игра $gameNum | Стол $tableNum"
    }

    /**
     * Crawls active games and updates them with latest data from MafiaUniverse
     */
    fun crawl() {
        gameRepository.findGameByTypeAndStarted(GameType.MAFIAUNIVERSE, true).forEach { game ->
            try {
                val tournamentId = game.tournamentId ?: return@forEach
                val gameNum = game.gameNum ?: return@forEach
                val tableNum = game.tableNum ?: return@forEach

                // Get games list to find the game ID
                val gamesListHtml = mafiaUniverseClient.getGamesListPage(tournamentId)
                val games = htmlParser.parseGamesList(gamesListHtml)

                val matchingGame = games.find {
                    it.tourNumber == gameNum && it.tableNumber == tableNum
                } ?: return@forEach

                // Fetch game details
                val gameDetailsHtml = mafiaUniverseClient.getGameDetailsPage(matchingGame.gameId)
                val parsedDetails = htmlParser.parseGameDetails(gameDetailsHtml)

                logCrawlSuccess(game, parsedDetails)

                // Update players
                game.players.forEach { player ->
                    val parsedPlayer = parsedDetails.players.find { it.position == player.place }
                    if (parsedPlayer != null) {
                        val playerEntity =
                            playerService.findOrCreatePlayerByMafiaUniverseNickname(parsedPlayer.nickname)
                        val effectiveNickname = playerService.getEffectiveNickname(playerEntity)
                        val playerPhotoUrl = photoService.getPlayerPhotoUrlForPlayerCompetitionRole(
                            nickname = parsedPlayer.nickname,
                            tournamentType = GameType.MAFIAUNIVERSE,
                            tournamentId = tournamentId.toLong(),
                            role = parsedPlayer.role
                        )

                        player.nickname = effectiveNickname
                        player.role = parsedPlayer.role
                        player.photoUrl = playerPhotoUrl
                    }
                }

                // Update game result
                if (parsedDetails.winner != null) {
                    game.result = parsedDetails.winner
                    game.started = false
                } else if (game.result != null) {
                    // Game was finished but now appears unfinished
                    game.result = null
                    game.players.forEach { it.score = null }
                }

                // Check if game should stop crawling
                if (!emitterService.hasEmittersForGame(game.id.toString())) {
                    game.started = false
                    getLogger().info("No emitters for game ${game.id}")
                }

                saveAndEmitGame(game)
            } catch (e: Exception) {
                handleCrawlError(game, e)
            }
        }
    }

    /**
     * Saves game and emits update via SSE
     */
    private fun saveAndEmitGame(game: Game) {
        try {
            // Ensure bidirectional association before save
            game.players.forEach { if (it.game == null) it.game = game }
            gameRepository.save(game)
        } catch (e: Exception) {
            getLogger().warn("Error while saving game ${game.id}: ${e.message}")
            throw e
        }

        // Get delay from game property
        val delaySeconds = game.delay

        if (delaySeconds > 0) {
            val gameCopy = game.copy()
            getLogger().info("Scheduling game update with delay of ${delaySeconds}s for ${game.id}")

            taskExecutorService.schedule(
                { emitterService.emitGame(gameCopy) },
                delaySeconds.toLong(),
                TimeUnit.SECONDS
            )
        } else {
            emitterService.emitGame(game)
        }
    }

    /**
     * Handles crawl errors with different strategies based on error type
     */
    private fun handleCrawlError(game: Game, exception: Exception) {
        when {
            // HTTP 404 - game deleted in MafiaUniverse
            isGameNotFoundError(exception) -> {
                game.started = false
                game.crawlStopReason = "GAME_DELETED_IN_MAFIAUNIVERSE"
                game.lastCrawlError = "Game not found in MafiaUniverse"
                getLogger().info("Game ${game.id} stopped - deleted in MafiaUniverse")
            }

            // Network errors and timeouts
            isNetworkError(exception) -> {
                val currentCount = (game.crawlFailureCount ?: 0) + 1
                game.crawlFailureCount = currentCount
                game.lastCrawlError = "Network error: ${exception.message}"
                game.lastFailureTime = LocalDateTime.now()

                if (currentCount >= 3) {
                    game.started = false
                    game.crawlStopReason = "NETWORK_ERRORS"
                    getLogger().warn("Game ${game.id} stopped after $currentCount network failures")
                }
            }

            // HTML parsing errors
            isParsingError(exception) -> {
                val currentCount = (game.crawlFailureCount ?: 0) + 1
                game.crawlFailureCount = currentCount
                game.lastCrawlError = "HTML parsing error: ${exception.message}"
                game.lastFailureTime = LocalDateTime.now()

                if (currentCount >= 2) {
                    game.started = false
                    game.crawlStopReason = "PARSING_ERRORS"
                    getLogger().error("Game ${game.id} stopped after $currentCount parsing failures", exception)
                }
            }

            // Other errors
            else -> {
                val currentCount = (game.crawlFailureCount ?: 0) + 1
                game.crawlFailureCount = currentCount
                game.lastCrawlError = "Unknown error: ${exception.message}"
                game.lastFailureTime = LocalDateTime.now()

                if (currentCount >= 2) {
                    game.started = false
                    game.crawlStopReason = "UNKNOWN_ERRORS"
                    getLogger().error("Game ${game.id} stopped after $currentCount unknown failures", exception)
                }
            }
        }

        gameRepository.save(game)
    }

    /**
     * Checks if error is "game not found"
     */
    private fun isGameNotFoundError(exception: Exception): Boolean {
        return exception is MafiaUniverseClient.GameNotFoundException ||
            exception.message?.contains("404") == true ||
            exception.message?.contains("Not Found") == true ||
            exception.message?.contains("not found", ignoreCase = true) == true
    }

    /**
     * Checks if error is a network error
     */
    private fun isNetworkError(exception: Exception): Boolean {
        return exception is ConnectException ||
            exception is SocketTimeoutException ||
            exception.message?.contains("timeout", ignoreCase = true) == true ||
            exception.message?.contains("Connection", ignoreCase = true) == true
    }

    /**
     * Checks if error is a parsing error
     */
    private fun isParsingError(exception: Exception): Boolean {
        return exception is IllegalArgumentException ||
            exception.message?.contains("not found", ignoreCase = true) == true ||
            exception.message?.contains("parse", ignoreCase = true) == true
    }

    /**
     * Logs successful crawl and resets error counters
     */
    private fun logCrawlSuccess(game: Game, parsedDetails: ParsedGameDetails) {
        val failureCount = game.crawlFailureCount ?: 0
        if (failureCount > 0) {
            getLogger().info("Game ${game.id} crawling recovered after $failureCount failures")
            game.crawlFailureCount = null
            game.lastCrawlError = null
            game.lastFailureTime = null
        }

        getLogger().info("MafiaUniverse game ${parsedDetails.tournamentId} tour ${parsedDetails.tourNumber} table ${parsedDetails.tableNumber} crawled successfully")
    }

    /**
     * Restarts game crawling manually
     */
    fun restartGameCrawling(gameId: UUID): Boolean {
        val game = gameRepository.findById(gameId).orElse(null) ?: return false

        // Reset error counters
        game.crawlFailureCount = null
        game.lastCrawlError = null
        game.lastFailureTime = null
        game.crawlStopReason = null
        if (game.manuallyStarted != false) {
            game.started = true
        }

        gameRepository.save(game)
        getLogger().info("Game ${game.id} crawling restarted manually")
        return true
    }
}
