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
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(value = ["app.polemicaEnable"], havingValue = "true")
class PolemicaService(
    val polemicaClient: PolemicaClient,
    val gameRepository: GameRepository,
    val emitterService: EmitterService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val gameIdCache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(256)
        .build<PolemicaTournamentGame, Long>()

    fun crawl() {
        gameRepository.findGameByTypeAndStarted(GameType.POLEMICA, true).forEach { game ->
            val tournamentId = game.tournamentId
            val gameNum = game.gameNum
            val tableNum = game.tableNum
            if (tournamentId == null || gameNum == null || tableNum == null) return@forEach
            val tournamentGame = PolemicaTournamentGame(tournamentId, gameNum, tableNum)
            val idO = gameIdCache.getIfPresent(tournamentGame)
            var id: Long? = null
            if (idO == null) {
                polemicaClient.getGamesFromCompetition(tournamentId.toLong()).forEach { tGame ->
                    val polemicaTournamentGame =
                        PolemicaTournamentGame(tournamentId, tGame.num.toInt(), tGame.table.toInt())
                    if (polemicaTournamentGame == tournamentGame) {
                        id = tGame.id
                    }
                    gameIdCache.put(polemicaTournamentGame, tGame.id)
                }
            } else {
                id = idO
            }
            log.info("Polemica game ${id} for $tournamentGame crawled")
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
            if (polemicaGame.result != null) {
                game.started = false
                gameRepository.save(game)
                val nextGame =
                    gameRepository.findGameByTournamentIdAndGameNumAndTableNum(tournamentId, gameNum + 1, tableNum)
                        ?: return
                nextGame.started = true
                gameRepository.save(nextGame)
                emitterService.sendTo(game.id.toString(), "!nextgame")
                return
            }
            game.players.forEach { player ->
                Position.fromInt(player.place)?.let { position ->
                    kicked[position]?.let {
                        val status = if (firstKilled != position) kickReasonToStatus(it.reason) else "first-killed"
                        player.status = Pair(status, "")
                    }
                    player.role = polemicaRoleToRole(polemicaGame.getRole(position))
                    player.guess = polemicaGuessToGuess(polemicaGame.players.find { it.position == position }?.guess)
                    if (player.role == "sher") {
                        val checks = polemicaGame.checks.filter { it.role == Role.SHERIFF }.sortedBy { it.night }
                        player.checks = checks.map {
                            mapOf(
                                "first" to polemicaColorToString(polemicaGame.getRole(it.player).isBlack()),
                                "second" to it.player.value.toString()
                            )
                        }.toMutableList()
                    }
                }
            }
            gameRepository.save(game)
            emitterService.emitGame(game.id.toString())
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

    data class PolemicaTournamentGame(val tournamentId: Int, val gameNum: Int, val tableNum: Int)
}
