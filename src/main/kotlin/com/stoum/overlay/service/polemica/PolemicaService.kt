package com.stoum.overlay.service.polemica

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.mafia.vyasma.polemica.library.client.GamePointsService
import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGuess
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.model.game.StageType
import com.github.mafia.vyasma.polemica.library.utils.KickReason
import com.github.mafia.vyasma.polemica.library.utils.getFirstKilled
import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.getVotingParticipants
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(value = ["app.polemicaEnable"], havingValue = "true")
class PolemicaService(
    val polemicaClient: PolemicaClient,
    val gameRepository: GameRepository,
    val emitterService: EmitterService,
    val pointsService: GamePointsService
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
                tryCreateGame(
                    tournamentId,
                    polemicaTournamentGame.gameNum,
                    polemicaTournamentGame.tableNum,
                    polemicaTournamentGame.phase
                )
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
            GamePlayer(
                id = null,
                nickname = it.username,
                place = it.position.value,
                photoUrl = polemicaPhotoUrl(it),
                role = "red",
                checks = arrayListOf(),
                guess = arrayListOf(),
                stat = mutableMapOf()
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
            text = "${polemicaTournament!!.name} | Игра ${polemicaTournamentGame.gameNum}"
        )
        return gameRepository.save(game)
    }

    private fun polemicaPhotoUrl(it: PolemicaPlayer?) =
        "https://storage.yandexcloud.net/mafia-photos/${it?.player?.id}.jpg"

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
                getLogger().info("Polemica game ${polemicaGame.id} for $tournamentGame crawled")
                val kicked = polemicaGame.getKickedFromTable().groupBy { it.position }.mapValues { it.value.first() }
                val firstKilled = polemicaGame.getFirstKilled()

                game.players.forEach { player ->
                    Position.fromInt(player.place)?.let { position ->
                        player.status = kicked[position]?.let {
                            if (firstKilled != position) kickReasonToStatus(it.reason) else "first-killed"
                        }
                        player.role = polemicaRoleToRole(polemicaGame.getRole(position))
                        val polemicaPlayer = polemicaGame.players?.find { it.position == position }
                        player.nickname = polemicaPlayer?.username.toString()
                        player.photoUrl = polemicaPhotoUrl(polemicaPlayer)
                        player.fouls = polemicaPlayer?.fouls?.size
                        player.techs = polemicaPlayer?.techs?.size
                        player.guess =
                            polemicaGuessToGuess(polemicaGame.players!!.find { it.position == position }?.guess)
                        if (player.role == "sher") {
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
                }


                if (polemicaGame.result != null) {
                    game.started = false
                    game.result = if (polemicaGame.result == PolemicaGameResult.RED_WIN) "red" else "black"
                    schedulePoints(game, id)
                    saveAndEmitGame(game)
                    val nextGame = getNextGame(tournamentGame) ?: return@forEach
                    nextGame.started = true
                    gameRepository.save(nextGame)
                    scheduleNextGameTasks(game.id, nextGame)
                    return@forEach
                }
                if (!emitterService.hasEmittersForGame(game.id.toString())) {
                    game.started = false
                    getLogger().info("No emitters for game ${game.id}")
                }
                saveAndEmitGame(game)
            } catch (e: Exception) {
                getLogger().error(
                    "Error while crawling polemica game {}: {} {} {}",
                    game.id, game.tournamentId, game.gameNum, game.tableNum, e
                )
                gameIdCache.invalidateAll()
            }
        }
    }

    private fun getPoints(game: Game, polemicaGameId: Long) {
        try {
            val points = pointsService.fetchPlayerStats(polemicaGameId)
            points.forEach { point ->
                game.players.find { it.place == point.position }?.score = point.points
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


    fun getNextGame(polemicaTournamentGame: PolemicaTournamentGame): Game? {
        with(polemicaTournamentGame) {
            val nextGameNum = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
                tournamentId,
                gameNum + 1,
                tableNum,
                phase,
                GameType.POLEMICA
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
            if (polemicaGamesCount > dbGamesCount) {
                initTournament(tournamentId)
                return getNextGame(polemicaTournamentGame)
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
