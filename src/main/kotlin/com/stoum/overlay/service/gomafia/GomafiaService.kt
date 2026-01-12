package com.stoum.overlay.service.gomafia

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.getLogger
import com.stoum.overlay.model.gomafia.UserWithStats
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.repository.PlayerRepository
import com.stoum.overlay.service.DEFAULT_PHOTO_URL
import com.stoum.overlay.service.PlayerPhotoService
import com.stoum.overlay.service.PlayerService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class GomafiaService(
        val gameRepository: GameRepository,
        val gomafiaRestClient: GomafiaRestClient,
        val playerRepository: PlayerRepository,
    val photoService: PlayerPhotoService,
    val playerService: PlayerService
) {
    val log = this.getLogger()

    @Transactional
    fun initTournament(tournamentId: Int, startWithGameNum: Int) {
        val tournament = gomafiaRestClient.getTournament(tournamentId)
        tournament.games.filter { g -> g.gameNum!! >= startWithGameNum }.forEach { gameDto ->
            log.info("processing $gameDto")
            val game = Game(
                type = GameType.GOMAFIA,
                tournamentId = tournamentId,
                gameNum = gameDto.gameNum,
                tableNum = gameDto.tableNum,
                phase = 0,
                text = "${tournament.tournamentDto.title} | Стол ${gameDto.tableNum} | Игра ${gameDto.gameNum}"
            )
            gameDto.table.forEach { playerDto ->
                val gomafiaPlayerId = playerDto.id?.toLong()
                val playerEntity = gomafiaPlayerId?.let { gid ->
                    playerService.findOrCreatePlayer(
                        nickname = playerDto.login!!,
                        gomafiaId = gid
                    )
                }
                val effectiveNickname =
                    playerEntity?.let { playerService.getEffectiveNickname(it) } ?: playerDto.login!!
                val playerPhotoUrl = gomafiaPlayerId?.let { playerId ->
                    photoService.getPlayerPhotoUrlForPlayerCompetitionRole(
                        playerId = playerId,
                        tournamentType = GameType.GOMAFIA,
                        tournamentId = tournamentId.toLong(),
                        role = "red"
                    )
                } ?: DEFAULT_PHOTO_URL
                val player = GamePlayer(
                    nickname = effectiveNickname,
                    photoUrl = playerPhotoUrl,
                    role = "red",
                    place = playerDto.place!!,
                    checks = mutableListOf(),
                    customPhoto = false,
                    sourcePlayerId = gomafiaPlayerId
                )
                player.game = game
                game.players.add(player)
            }
            gameRepository.save(game)
        }
    }

    @Transactional
    fun getGame(tournamentId: Int, gameNum: Int, tableNum: Int): Game? {
        log.info("Getting game tournamentId: ${tournamentId}, gameNum: ${gameNum}, tableNum: $tableNum")
        if (gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
                tournamentId,
                gameNum,
                tableNum,
                0,
                GameType.GOMAFIA
            ) == null
        ) {
            log.info("Game not found, initiating tournament")
            initTournament(tournamentId, gameNum)
        }
        return gameRepository.findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
            tournamentId,
            gameNum,
            tableNum,
            0,
            GameType.GOMAFIA
        )
    }

    private fun extractStat(us: UserWithStats): MutableMap<String, Map<String, String>> {
        return mutableMapOf(
                "red" to mapOf("first" to "${us.stats.winRate!!.red!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.red["per_game"]}"),
                "black" to mapOf("first" to "${us.stats.winRate!!.mafia!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.black["per_game"]}"),
                "sher" to mapOf("first" to "${us.stats.winRate!!.sheriff!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.sheriff["per_game"]}"),
                "don" to mapOf("first" to "${us.stats.winRate!!.don!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.black["per_game"]}"),
                "header" to mapOf("first" to "${us.stats.winRate!!.totalWins!!.percent}%", "second" to "${us.stats.advancedPoints!!.points10Games}")
        )
    }
}
