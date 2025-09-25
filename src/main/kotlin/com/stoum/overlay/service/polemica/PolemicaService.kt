package com.stoum.overlay.service.polemica

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.mafia.vyasma.polemica.library.client.GamePointsService
import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGuess
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.model.game.StageType
import com.github.mafia.vyasma.polemica.library.utils.KickReason
import com.github.mafia.vyasma.polemica.library.utils.getFinalVotes
import com.github.mafia.vyasma.polemica.library.utils.getFirstKilled
import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.getVoteCandidatesOrder
import com.github.mafia.vyasma.polemica.library.utils.getVotingParticipants
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.DEFAULT_PHOTO_URL
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.PlayerPhotoService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(value = ["app.polemicaEnable"], havingValue = "true")
class PolemicaService(
    val polemicaClient: PolemicaClient,
    val gameRepository: GameRepository,
    val emitterService: EmitterService,
    val pointsService: GamePointsService,
    val photoService: PlayerPhotoService
) {
    private val taskExecutorService = Executors.newScheduledThreadPool(4)
    private val gameIdCache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(1024)
        .build<PolemicaTournamentGame, Long>()

    fun getOrTryCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        getLogger().info("Getting game tournamentId: $tournamentId, gameNum: $gameNum, tableNum: $tableNum, phase: $phase")
        var game = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
            tournamentId,
            gameNum,
            tableNum,
            phase,
            GameType.POLEMICA
        )
        if (game == null) {
            game = tryCreateGame(tournamentId, gameNum, tableNum, phase)
            taskExecutorService.submit { initTournament(tournamentId) }
        }
        if (game != null) {
            game.started = true
            gameRepository.save(game)
        }
        return game
    }

    fun getPolemicaGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): PolemicaGame? {
        val tournamentGame = PolemicaTournamentGame(tournamentId, gameNum, tableNum, phase)
        polemicaClient.getGamesFromCompetition(tournamentId.toLong()).forEach { tGame ->
            val polemicaTournamentGame = PolemicaTournamentGame(tournamentId, tGame)
            if (polemicaTournamentGame == tournamentGame) {
                return polemicaClient.getGameFromCompetition(
                    PolemicaClient.PolemicaCompetitionGameId(
                        polemicaTournamentGame.tournamentId.toLong(),
                        tGame.id,
                        4
                    )
                )
            }
        }
        return null
    }

    private fun initTournament(tournamentId: Int) {
        polemicaClient.getGamesFromCompetition(tournamentId.toLong()).forEach { tGame ->
            val polemicaTournamentGame = PolemicaTournamentGame(tournamentId, tGame)
            val game = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
                tournamentId,
                polemicaTournamentGame.gameNum,
                polemicaTournamentGame.tableNum,
                polemicaTournamentGame.phase,
                GameType.POLEMICA
            )
            if (game == null) {
                createGameFromPolemica(PolemicaTournamentGame(tournamentId, tGame), tGame.id)
            }
        }
    }

    private fun tryCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        val tournamentGame = PolemicaTournamentGame(tournamentId, gameNum, tableNum, phase)
        polemicaClient.getGamesFromCompetition(tournamentId.toLong()).forEach { tGame ->
            val polemicaTournamentGame = PolemicaTournamentGame(tournamentId, tGame)
            if (polemicaTournamentGame == tournamentGame) {
                return createGameFromPolemica(polemicaTournamentGame, tGame.id)
            }
        }
        return null
    }

    /**
     * Определяет количество столов в указанной фазе турнира
     */
    private fun getTablesCountInPhase(tournamentId: Int, phase: Int): Int {
        return gameRepository.findGamesByTournamentId(tournamentId)
            .filter { it.phase == phase && it.type == GameType.POLEMICA }
            .map { it.tableNum }
            .distinct()
            .size
    }

    /**
     * Генерирует название игры согласно новым требованиям:
     * - Название турнира | Игра N (один стол)
     * - Название турнира | Игра N | Стол M (несколько столов)
     * - Название турнира | Финал | Игра N (финал с одним столом)
     * - Название турнира | Финал | Игра N | Стол M (финал с несколькими столами)
     */
    private fun generateGameTitle(
        tournamentName: String,
        gameNum: Int,
        tableNum: Int,
        phase: Int,
        tournamentId: Int
    ): String {
        val parts = mutableListOf<String>()

        // Название турнира
        parts.add(tournamentName)

        // Финал (если phase = 2)
        if (phase == 2) {
            parts.add("Финал")
        }

        // Номер игры
        parts.add("Игра $gameNum")

        // Номер стола (если в фазе больше одного стола)
        val tablesCount = getTablesCountInPhase(tournamentId, phase)
        if (tablesCount > 1) {
            parts.add("Стол $tableNum")
        }

        return parts.joinToString(" | ")
    }

    private fun createGameFromPolemica(polemicaTournamentGame: PolemicaTournamentGame, polemicaGameId: Long): Game {
        val polemicaTournament = polemicaClient.getCompetition(polemicaTournamentGame.tournamentId.toLong())
        val polemicaGame = polemicaClient.getGameFromCompetition(
            PolemicaClient.PolemicaCompetitionGameId(
                polemicaTournamentGame.tournamentId.toLong(),
                polemicaGameId,
                4
            )
        )
        val players = polemicaGame.players?.sortedBy { it.position.value }?.map {
            val photoUrl = it.player?.id?.let { playerId ->
                photoService.getPlayerPhotoUrlForPlayerCompetitionRole(
                    playerId = playerId,
                    tournamentType = GameType.POLEMICA,
                    tournamentId = polemicaTournamentGame.tournamentId.toLong(),
                    role = polemicaRoleToRole(Role.PEACE)
                )
            } ?: DEFAULT_PHOTO_URL
            GamePlayer(
                id = null,
                nickname = it.username,
                place = it.position.value,
                photoUrl = photoUrl,
                role = "red",
                checks = arrayListOf(),
                guess = arrayListOf(),
                stat = mutableMapOf(),
                customPhoto = false,
                sourcePlayerId = it.player?.id
            )
        }?.toMutableList() ?: mutableListOf()
        val game = Game(
            id = null,
            type = GameType.POLEMICA,
            tournamentId = polemicaTournamentGame.tournamentId,
            gameNum = polemicaTournamentGame.gameNum,
            tableNum = polemicaTournamentGame.tableNum,
            phase = polemicaTournamentGame.phase,
            players = players,
            started = true,
            visibleOverlay = true,
            visibleRoles = true,
            text = generateGameTitle(
                tournamentName = polemicaTournament!!.name,
                gameNum = polemicaTournamentGame.gameNum,
                tableNum = polemicaTournamentGame.tableNum,
                phase = polemicaTournamentGame.phase,
                tournamentId = polemicaTournamentGame.tournamentId
            )
        )
        return gameRepository.save(game)
    }

    fun crawl() {
        gameRepository.findGameByTypeAndStarted(GameType.POLEMICA, true).forEach { game ->
            try {
                val (tournamentId, _, _, _) = with(game) {
                    listOfNotNull(tournamentId, gameNum, tableNum, phase)
                        .takeIf { it.size == 4 } ?: return@forEach
                }
                val tournamentGame = PolemicaTournamentGame(game)
                var idO = gameIdCache.getIfPresent(tournamentGame)
                if (idO == null) {
                    polemicaClient.getGamesFromCompetition(tournamentId.toLong()).forEach { tGame ->
                        val polemicaTournamentGame = PolemicaTournamentGame(tournamentId, tGame)
                        if (polemicaTournamentGame == tournamentGame) {
                            idO = tGame.id
                        }
                        gameIdCache.put(polemicaTournamentGame, tGame.id)
                    }
                }
                val id = idO ?: return@forEach
                val polemicaGame = polemicaClient.getGameFromCompetition(
                    PolemicaClient.PolemicaCompetitionGameId(
                        tournamentId.toLong(),
                        id,
                        4
                    )
                )
                logCrawlSuccess(game, polemicaGame)
                val kicked = polemicaGame.getKickedFromTable().groupBy { it.position }.mapValues { it.value.first() }
                val firstKilled = polemicaGame.getFirstKilled()
                val getPolemicaRoles = polemicaGame.stage?.type != StageType.DEALING

                game.players.forEach { player ->
                    Position.fromInt(player.place)?.let { position ->
                        val polemicaPlayer = polemicaGame.players?.find { it.position == position }
                        player.status = kicked[position]?.let {
                            if (it.reason == KickReason.VOTING) {
                                player.votedBy =
                                    polemicaGame.getFinalVotes(beforeGamePhase = null).filter { it.expelled }
                                        .filter { it.convicted.contains(position) }.map {
                                            mapOf(
                                                "role" to polemicaRoleToRole(polemicaGame.getRole(it.position)),
                                                "num" to it.position.value.toString()
                                            )
                                        }.toMutableList()
                            }
                            if (firstKilled != position) kickReasonToStatus(it.reason) else "first-killed"
                        }
                        if (getPolemicaRoles) {
                            player.role = polemicaRoleToRole(polemicaGame.getRole(position))
                        }
                        val playerPhotoUrl = polemicaPlayer?.player?.id?.let { playerId ->
                            photoService.getPlayerPhotoUrlForPlayerCompetitionRole(
                                playerId = playerId,
                                tournamentType = GameType.POLEMICA,
                                tournamentId = tournamentId.toLong(),
                                role = if (game.visibleRoles == true) player.role else null
                            )
                        } ?: DEFAULT_PHOTO_URL
                        player.nickname = polemicaPlayer?.username.toString()
                        player.photoUrl = playerPhotoUrl
                        player.fouls = polemicaPlayer?.fouls?.size
                        player.techs = polemicaPlayer?.techs?.size
                        player.guess =
                            polemicaGuessToGuess(polemicaGame.players!!.find { it.position == position }?.guess)
                        if (player.role == polemicaRoleToRole(Role.SHERIFF)) {
                            val checks = polemicaGame.checks?.filter { it.role == Role.SHERIFF }?.sortedBy { it.night }
                            player.checks = checks?.map {
                                mapOf(
                                    "role" to polemicaRoleToRole(polemicaGame.getRole(it.player)),
                                    "num" to it.player.value.toString()
                                )
                            }?.toMutableList()
                        }
                        if (player.role == "don") {
                            val checks = polemicaGame.checks?.filter { it.role == Role.DON }?.sortedBy { it.night }
                            player.checks = checks?.map {
                                mapOf(
                                    "role" to polemicaRoleToRole(polemicaGame.getRole(it.player)),
                                    "num" to it.player.value.toString()
                                )
                            }?.toMutableList()
                        }
                    }
                }

                // Сброс состояния игроков
                game.players.forEach { player ->
                    player.speaker = false
                    player.voting = false
                }

                // Обработка только если есть активная стадия
                polemicaGame.stage?.let { stage ->
                    // Определение говорящего игрока
                    if (stage.type in listOf(
                            StageType.SPEECH,
                            StageType.RESPEECH,
                            StageType.VOTED,
                            StageType.SHOOTED
                        )
                    ) {
                        game.players.find { it.place == stage.player }?.speaker = true
                    }

                    // Определение участников голосования
                    val votingParticipants = when {
                        stage.type == StageType.SPEECH ->
                            polemicaGame.getVotingParticipants(stage.day, 1)

                        stage.voting != null && stage.type == StageType.VOTING ->
                            polemicaGame.getVotingParticipants(stage.day, stage.voting!!)

                        stage.voting != null ->
                            polemicaGame.getVotingParticipants(stage.day, stage.voting!! + 1)

                        else -> emptyList()
                    }

                    // Пометка игроков, участвующих в голосовании
                    val votingPlaces = votingParticipants.map { it.value }
                    game.players.filter { it.place in votingPlaces }.forEach { it.voting = true }

                    if (stage.day >= 1) {
                        game.voteCandidates = polemicaGame.getVoteCandidatesOrder(stage.day)
                            .filter { votingParticipants.contains(it) }
                            .map {
                                mapOf(
                                    "role" to polemicaRoleToRole(polemicaGame.getRole(it)),
                                    "num" to it.value.toString()
                                )
                            }.toMutableList()
                    }

                }



                if (polemicaGame.result != null) {
                    game.started = false
                    game.result = if (polemicaGame.result == PolemicaGameResult.RED_WIN) "red" else "black"
                    schedulePoints(game, id)
                    saveAndEmitGame(game)
                    val nextGame = getNextGame(tournamentGame) ?: return@forEach
                    nextGame.started = true
                    gameRepository.save(nextGame)
                    if (game.autoNextGame != false) {
                        scheduleNextGameTasks(game.id, nextGame)
                    }
                    return@forEach
                } else if (game.result != null) {
                    game.result = null
                    game.players.forEach { it.score = null }
                }
                if (!emitterService.hasEmittersForGame(game.id.toString())) {
                    game.started = false
                    getLogger().info("No emitters for game ${game.id}")
                }
                saveAndEmitGame(game)
            } catch (e: Exception) {
                handleCrawlError(game, e)
                gameIdCache.invalidateAll()
            }
        }
    }

    private fun getPoints(game: Game, polemicaGameId: Long) {
        try {
            val points = pointsService.fetchPlayerStats(polemicaGameId)
            points.forEach { point ->
                game.players.find { it.place == point.position }?.score = Math.round(point.points * 1000) / 1000.0;
            }
        } catch (e: Exception) {
            getLogger().warn("Error while fetching points for game ${game.id}: ${e.message}")
        }
    }

    private fun saveAndEmitGame(game: Game) {
        // Сохраняем игру в репозиторий
        try {
            gameRepository.save(game)
        } catch (e: Exception) {
            getLogger().warn("Error while saving game ${game.id}: ${e.message}")
            throw e
        }

        // Получаем задержку из свойства game
        val delaySeconds = game.delay

        if (delaySeconds > 0) {
            val gameCopy = game.copy()
            getLogger().info("Scheduling game update with delay of ${delaySeconds}s for ${game.id}")

            // Используем taskExecutorService для отложенной отправки
            taskExecutorService.schedule({ emitterService.emitGame(gameCopy) }, delaySeconds.toLong(), TimeUnit.SECONDS)
        } else {
            // Если задержка не требуется, отправляем сразу
            emitterService.emitGame(game)
        }
    }

    fun getNextGame(polemicaTournamentGame: PolemicaTournamentGame, withInit: Boolean = true): Game? {
        with(polemicaTournamentGame) {
            val nextGameNum = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
                tournamentId = tournamentId,
                gameNum = gameNum + 1,
                tableNum = tableNum,
                phase = phase,
                type = GameType.POLEMICA
            )
            if (nextGameNum != null) {
                return nextGameNum
            }
            val nextPhaseGame = gameRepository.findGamesByTournamentIdAndGameNumAndTableNum(tournamentId, 1, tableNum)
                .filter { it.phase != null }
                .filter { it.phase!! > phase }
                .minByOrNull { it.phase!! }
            if (nextPhaseGame != null) {
                return nextPhaseGame
            }
            val polemicaGamesCount = polemicaClient.getGamesFromCompetition(tournamentId.toLong()).size
            val dbGamesCount = gameRepository.findGamesByTournamentId(tournamentId).size
            if (polemicaGamesCount > dbGamesCount && withInit) {
                initTournament(tournamentId)
                return getNextGame(polemicaTournamentGame, false)
            }
            return null
        }
    }

    private fun polemicaGuessToGuess(guess: PolemicaGuess?): MutableList<Map<String, String>> {
        val guessList = mutableListOf<Map<String, String>>()
        guess?.vice?.let { guessList.add(mapOf("role" to "vice", "num" to it.value.toString())) }
        guess?.civs?.forEach { guessList.add(mapOf("role" to "red", "num" to it.value.toString())) }
        guess?.mafs?.forEach { guessList.add(mapOf("role" to "black", "num" to it.value.toString())) }
        return guessList
    }

    fun kickReasonToStatus(kickReason: KickReason) = when (kickReason) {
        KickReason.KILL -> "killed"
        KickReason.VOTING -> "voted"
        KickReason.DISQUAL -> "deleted"
    }

    fun polemicaRoleToRole(role: Role) = when (role) {
        Role.MAFIA -> "black"
        Role.DON -> "don"
        Role.SHERIFF -> "sher"
        Role.PEACE -> "red"
    }

    fun roleToPolemicaRole(role: String?) = when (role) {
        "black" -> Role.MAFIA
        "don" -> Role.DON
        "sher" -> Role.SHERIFF
        "red" -> Role.PEACE
        else -> Role.PEACE
    }

    fun polemicaColorToString(isBlack: Boolean) = if (isBlack) "black" else "red"

    fun scheduleNextGameTasks(gameId: UUID?, nextGame: Game) {
        val delays = longArrayOf(60, 70, 80, 90, 120, 150, 180, 240, 300)

        // Базовая задержка из свойства nextGame
        val baseDelay = nextGame.delay.toLong()

        for (delaySeconds in delays) {
            // Учитываем базовую задержку
            val totalDelay = baseDelay + delaySeconds

            taskExecutorService.schedule(
                { emitterService.changeGame(gameId.toString(), nextGame) },
                totalDelay,
                TimeUnit.SECONDS
            )
        }
    }

    fun schedulePoints(game: Game, polemicaId: Long) {
        val delays = longArrayOf(0, 1, 3, 5, 8)
        for (delaySeconds in delays) {
            taskExecutorService.schedule(
                {
                    val gameC = game.id?.let { gameRepository.getReferenceById(it) } ?: return@schedule
                    getPoints(gameC, polemicaId)
                    saveAndEmitGame(gameC)
                },
                delaySeconds,
                TimeUnit.SECONDS
            )
        }

        fun getAllCompetitions(): List<PolemicaClient.PolemicaCompetition> {
            return polemicaClient.getCompetitions()
        }
    }

    fun clearRoles(gameId: UUID) {
        val game = gameRepository.findById(gameId).orElse(null) ?: return
        game.players.forEach {
            it.role = null
        }
        saveAndEmitGame(game)
    }

    fun updateRoles(gameId: UUID, roles: Map<Int, String>) {
        val game = gameRepository.findById(gameId).orElse(null) ?: return
        game.players.forEach { player ->
            roles[player.place]?.let { newRole ->
                player.role = newRole
            }
        }
    }

    /**
     * Обработка ошибок краулинга с различением типов ошибок
     */
    private fun handleCrawlError(game: Game, exception: Exception) {
        when {
            // HTTP 404 - игра удалена в Полемике
            isGameNotFoundError(exception) -> {
                game.started = false
                game.crawlStopReason = "GAME_DELETED_IN_POLEMICA"
                game.lastCrawlError = "Game not found in Polemica API"
                getLogger().info("Game ${game.id} stopped - deleted in Polemica")
            }

            // HTTP 401/403 - проблемы с авторизацией
            isAuthenticationError(exception) -> {
                val currentCount = (game.crawlFailureCount ?: 0) + 1
                game.crawlFailureCount = currentCount
                game.lastCrawlError = "Authentication failed: ${exception.message}"
                game.lastFailureTime = LocalDateTime.now()

                if (currentCount >= 5) {
                    game.started = false
                    game.crawlStopReason = "AUTHENTICATION_FAILED"
                    getLogger().error("Game ${game.id} stopped - authentication failed")
                }
            }

            // Сетевые ошибки и таймауты
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

            // Другие ошибки
            else -> {
                val currentCount = (game.crawlFailureCount ?: 0) + 1
                game.crawlFailureCount = currentCount
                game.lastCrawlError = "Unknown error: ${exception.message}"
                game.lastFailureTime = LocalDateTime.now()

                if (currentCount >= 2) {
                    game.started = false
                    game.crawlStopReason = "UNKNOWN_ERRORS"
                    getLogger().error(
                        "Game ${game.id} stopped after $currentCount unknown failures",
                        exception
                    )
                }
            }
        }

        gameRepository.save(game)
    }

    /**
     * Проверка на ошибку "игра не найдена"
     */
    private fun isGameNotFoundError(exception: Exception): Boolean {
        return exception.message?.contains("404") == true ||
            exception.message?.contains("Not Found") == true ||
            exception is NoSuchElementException
    }

    /**
     * Проверка на ошибки авторизации
     */
    private fun isAuthenticationError(exception: Exception): Boolean {
        return exception.message?.contains("401") == true ||
            exception.message?.contains("403") == true ||
            exception.message?.contains("Unauthorized") == true
    }

    /**
     * Проверка на сетевые ошибки
     */
    private fun isNetworkError(exception: Exception): Boolean {
        return exception is ConnectException ||
            exception is SocketTimeoutException ||
            exception.message?.contains("timeout") == true
    }

    /**
     * Логирование успешного краулинга с сбросом счетчика ошибок
     */
    private fun logCrawlSuccess(game: Game, polemicaGame: PolemicaGame) {
        // Сброс счетчика при успешном краулинге
        val failureCount = game.crawlFailureCount ?: 0
        if (failureCount > 0) {
            getLogger().info("Game ${game.id} crawling recovered after $failureCount failures")
            game.crawlFailureCount = null
            game.lastCrawlError = null
            game.lastFailureTime = null
        }

        getLogger().info("Polemica game ${polemicaGame.id} for ${PolemicaTournamentGame(game)} crawled with stage ${polemicaGame.stage}")
    }

    /**
     * Попытка восстановить краулинг остановленной игры
     */
    fun restartGameCrawling(gameId: UUID): Boolean {
        val game = gameRepository.findById(gameId).orElse(null) ?: return false

        // Сброс счетчиков ошибок
        game.crawlFailureCount = null
        game.lastCrawlError = null
        game.lastFailureTime = null
        game.crawlStopReason = null
        game.started = true

        gameRepository.save(game)
        getLogger().info("Game ${game.id} crawling restarted manually")
        return true
    }

    /**
     * Автоматическое восстановление игр после определенного времени
     */
    fun autoRecoverStoppedGames() {
        val cutoffTime = LocalDateTime.now().minusHours(1)
        val stoppedGames = gameRepository.findGamesByTypeAndStartedAndCrawlStopReasonIsNotNull(
            GameType.POLEMICA, false
        ).filter {
            it.lastFailureTime?.isBefore(cutoffTime) == true &&
                it.crawlStopReason in listOf("NETWORK_ERRORS", "UNKNOWN_ERRORS")
        }

        stoppedGames.forEach { game ->
            getLogger().info("Auto-recovering game ${game.id} after network issues")
            restartGameCrawling(game.id!!)
        }
    }

    /**
     * Получить статистику по проблемным играм
     */
    fun getCrawlErrorStatistics(): Map<String, Any> {
        val allGames = gameRepository.findGameByTypeAndStarted(GameType.POLEMICA, false)
            .filter { it.crawlStopReason != null }

        return mapOf(
            "totalStopped" to allGames.size,
            "byReason" to allGames.groupBy { it.crawlStopReason }.mapValues { it.value.size },
            "recentFailures" to allGames.filter {
                it.lastFailureTime?.isAfter(LocalDateTime.now().minusHours(24)) == true
            }.size
        )
    }

    /**
     * Получить список проблемных игр для админ-панели
     */
    fun getProblematicGames(): List<Map<String, Any>> {
        return gameRepository.findGamesByTypeAndStartedAndCrawlFailureCountGreaterThan(
            GameType.POLEMICA, false, 0
        ).map { game ->
            mapOf(
                "id" to game.id,
                "tournamentId" to game.tournamentId,
                "gameNum" to game.gameNum,
                "tableNum" to game.tableNum,
                "failureCount" to game.crawlFailureCount,
                "lastError" to game.lastCrawlError,
                "stopReason" to game.crawlStopReason,
                "lastFailureTime" to game.lastFailureTime
            ) as Map<String, Any>
        }
    }

    data class PolemicaTournamentGame(val tournamentId: Int, val gameNum: Int, val tableNum: Int, val phase: Int) {
        constructor(game: Game) : this(game.tournamentId!!, game.gameNum!!, game.tableNum!!, game.phase!!)

        constructor(
            tournamentId: Int,
            tGame: PolemicaClient.PolemicaTournamentGameReference
        ) : this(
            tournamentId,
            tGame.num.toInt(),
            tGame.table.toInt(),
            tGame.phase.toInt()
        )
    }
}
