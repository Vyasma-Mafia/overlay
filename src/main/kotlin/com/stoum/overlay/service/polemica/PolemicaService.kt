package com.stoum.overlay.service.polemica

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGuess
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.KickReason
import com.github.mafia.vyasma.polemica.library.utils.getFirstKilled
import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    val emitterService: EmitterService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val taskExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val gameIdCache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(1024)
        .build<PolemicaTournamentGame, Long>()

    @Transactional
    fun getOrTryCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        log.info("Getting game tournamentId: $tournamentId, gameNum: $gameNum, tableNum: $tableNum, phase: $phase")
        val game = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhase(
            tournamentId,
            gameNum,
            tableNum,
            phase
        )
        if (game == null) {
            return tryCreateGame(tournamentId, gameNum, tableNum, phase)
        }
        return game
    }

    private fun tryCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        val tournamentGame = PolemicaTournamentGame(tournamentId, gameNum, tableNum, phase)
        polemicaClient.getGamesFromCompetition(tournamentId.toLong()).forEach { tGame ->
            val polemicaTournamentGame =
                PolemicaTournamentGame(
                    tournamentId,
                    tGame.num.toInt(),
                    tGame.table.toInt(),
                    tGame.phase.toInt()
                )
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
                photoUrl = "https://storage.yandexcloud.net/mafia-photos/${it.player}.jpg",
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

    fun crawl() {
        gameRepository.findGameByTypeAndStarted(GameType.POLEMICA, true).forEach { game ->
            try {
                val (tournamentId, gameNum, tableNum, phase) = with(game) {
                    listOfNotNull(tournamentId, gameNum, tableNum, phase)
                        .takeIf { it.size == 4 } ?: return@forEach
                }
                val tournamentGame = PolemicaTournamentGame(tournamentId, gameNum, tableNum, phase)
                val idO = gameIdCache.getIfPresent(tournamentGame)
                var id: Long? = null
                if (idO == null) {
                    polemicaClient.getGamesFromCompetition(tournamentId.toLong()).forEach { tGame ->
                        val polemicaTournamentGame =
                            PolemicaTournamentGame(
                                tournamentId,
                                tGame.num.toInt(),
                                tGame.table.toInt(),
                                tGame.phase.toInt()
                            )
                        if (polemicaTournamentGame == tournamentGame) {
                            id = tGame.id
                        }
                        gameIdCache.put(polemicaTournamentGame, tGame.id)
                    }
                } else {
                    id = idO
                }
                log.info("Polemica game $id for $tournamentGame crawled")
                if (id == null) return@forEach
                val polemicaGame = polemicaClient.getGameFromCompetition(
                    PolemicaClient.PolemicaCompetitionGameId(
                        tournamentId.toLong(),
                        id ?: 0L,
                        4
                    )
                )
                getLogger().info("Polemica game ${polemicaGame.id} in tournament $tournamentId crawled")
                val kicked = polemicaGame.getKickedFromTable().groupBy { it.position }.mapValues { it.value.first() }
                val firstKilled = polemicaGame.getFirstKilled()

                game.players.forEach { player ->
                    Position.fromInt(player.place)?.let { position ->
                        player.status = kicked[position]?.let {
                            Pair(if (firstKilled != position) kickReasonToStatus(it.reason) else "first-killed", "")
                        }
                        player.role = polemicaRoleToRole(polemicaGame.getRole(position))
                        player.guess =
                            polemicaGuessToGuess(polemicaGame.players!!.find { it.position == position }?.guess)
                        if (player.role == "sher") {
                            val checks = polemicaGame.checks?.filter { it.role == Role.SHERIFF }?.sortedBy { it.night }
                            player.checks = checks?.map {
                                mapOf(
                                    "first" to polemicaRoleToRole(polemicaGame.getRole(it.player)),
                                    "second" to it.player.value.toString()
                                )
                            }?.toMutableList()
                        }
                        if (player.role == "don") {
                            val checks = polemicaGame.checks?.filter { it.role == Role.DON }?.sortedBy { it.night }
                            player.checks = checks?.map {
                                mapOf(
                                    "first" to polemicaRoleToRole(polemicaGame.getRole(it.player)),
                                    "second" to it.player.value.toString()
                                )
                            }?.toMutableList()
                        }
                    }
                }
                if (polemicaGame.result != null) {
                    game.started = false
                    gameRepository.save(game)
                    val nextGame = getNextGame(tournamentGame) ?: return@forEach
                    nextGame.started = true
                    gameRepository.save(nextGame)
                    scheduleNextGameTasks(game.id)
                    return@forEach
                }
                gameRepository.save(game)
                emitterService.emitGame(game.id.toString())
            } catch (e: Exception) {
                getLogger().error(
                    "Error while crawling polemica game {}: {} {} {}",
                    game.id, game.tournamentId, game.gameNum, game.tableNum, e
                )
                gameIdCache.invalidateAll()
            }
        }
    }

    private fun getNextGame(polemicaTournamentGame: PolemicaTournamentGame): Game? {
        with(polemicaTournamentGame) {
            val nextGameNum = gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhase(
                tournamentId,
                gameNum + 1,
                tableNum,
                phase
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
            return null
        }
    }

    private fun polemicaGuessToGuess(guess: PolemicaGuess?): MutableList<Map<String, String>> {
        val guessList = mutableListOf<Map<String, String>>()
        guess?.vice?.let { guessList.add(mapOf("first" to "vice", "second" to it.value.toString())) }
        guess?.civs?.forEach { guessList.add(mapOf("first" to "red", "second" to it.value.toString())) }
        guess?.mafs?.forEach { guessList.add(mapOf("first" to "black", "second" to it.value.toString())) }
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

    fun scheduleNextGameTasks(gameId: UUID?) {
        val delays = longArrayOf(0, 10, 20, 30, 60, 90, 120, 150, 180)

        for (delayMinutes in delays) {
            taskExecutorService.schedule({
                emitterService.sendTo(gameId.toString(), "!nextgame")
            }, delayMinutes, TimeUnit.SECONDS)
        }
    }

    data class PolemicaTournamentGame(val tournamentId: Int, val gameNum: Int, val tableNum: Int, val phase: Int)
}
