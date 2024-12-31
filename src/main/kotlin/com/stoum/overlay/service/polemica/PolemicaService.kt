package com.stoum.overlay.service.polemica

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.KickReason
import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
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
    private val gameIdCache = Caffeine.newBuilder()
        .expireAfterWrite(180, TimeUnit.SECONDS)
        .maximumSize(100)
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
            if (polemicaGame.result != null) {
                game.started = false
                gameRepository.save(game)
                return
            }
            game.players.forEach { player ->
                Position.fromInt(player.place)?.let { position ->
                    kicked[position]?.let {
                        player.status = Pair(kickReasonToStatus(it.reason), "")
                    }
                    player.role = polemicaRoleToRole(polemicaGame.getRole(position))
                    if (player.role == "sher") {
                        val checks = polemicaGame.checks.filter { it.role == Role.SHERIFF }.sortedBy { it.night }
                        player.checks = checks.map {
                            mapOf(
                                "first" to polemicaColorToString(polemicaGame.getRole(position).isBlack()),
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
