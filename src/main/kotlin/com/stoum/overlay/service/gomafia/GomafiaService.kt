package com.stoum.overlay.service.gomafia

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.PlayerPhoto
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.getLogger
import com.stoum.overlay.model.gomafia.GameDto
import com.stoum.overlay.model.gomafia.UserWithStats
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.repository.PlayerRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Service
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestClient
import java.net.HttpURLConnection
import java.net.URL

@Service
class GomafiaService(
        val gameRepository: GameRepository,
        val gomafiaRestClient: GomafiaRestClient,
        val playerRepository: PlayerRepository,
) {
    val log = this.getLogger()

    @Transactional
    fun initTournament(tournamentId: Int, startWithGameNum: Int) {
        val tournament = gomafiaRestClient.getTournament(tournamentId)
        tournament.games.filter { g -> g.gameNum!! >= startWithGameNum }.forEach { gameDto ->
            log.info("processing $gameDto")
            val game = Game(type = GameType.FSM, tournamentId = tournamentId, gameNum = gameDto.gameNum, tableNum = gameDto.tableNum)
            gameDto.table.forEach { playerDto ->
                var player = playerRepository.findPlayerByNickname(playerDto.login)
                if (player == null) {
                    val u = gomafiaRestClient.getUserWithStats(playerDto.id!!)
                    player = Player(
                            nickname = u.user.login!!,
                            stat = extractStat(u),
                            //todo remove hack for cup of BO
                            playerPhotos = mutableListOf(PlayerPhoto(url = "/photo/${playerDto.login}.png", type = PhotoType.CUSTOM, description = "Custom photo"))
                    )
                    playerRepository.save(player)
                }
                game.players.add(GamePlayer(
                        nickname = playerDto.login!!,
                        photoUrl = player.playerPhotos.firstOrNull() { pp -> pp.type == PhotoType.CUSTOM }?.url ?: player.playerPhotos.first().url,
                        role = "red",
                        place = playerDto.place!!,
                        //status = "killed" to "$it",
                        checks = mutableListOf(),
                        stat = player.stat
                ))
            }

            gameRepository.save(game)
        }
    }

    @Transactional
    fun getGame(tournamentId: Int, gameNum: Int, tableNum: Int): Game? {
        log.info("Getting game tournamentId: ${tournamentId}, gameNum: ${gameNum}, tableNum: $tableNum")
        if(gameRepository.findGameByTournamentIdAndGameNumAndTableNum(tournamentId, gameNum, tableNum) == null) {
            log.info("Game not found, initiating tournament")
            initTournament(tournamentId, gameNum)
        }
        return gameRepository.findGameByTournamentIdAndGameNumAndTableNum(tournamentId, gameNum, tableNum)
    }

    private fun userWithStatsToGamePlayer(us: UserWithStats, game: GameDto, player: Player): GamePlayer {
        val gamePlayer = GamePlayer(
                nickname = us.user.login!!,
                photoUrl = us.user.avatar_link,
                role = "red",
                place = game.table.first { p -> p.login == us.user.login }.place!!,
                //status = "killed" to "$it",
                checks = mutableListOf(),
                stat = mutableMapOf(
                        "red" to mapOf("first" to "${us.stats.winRate!!.red!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.red["per_game"]}"),
                        "black" to mapOf("first" to "${us.stats.winRate!!.mafia!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.black["per_game"]}"),
                        "sher" to mapOf("first" to "${us.stats.winRate!!.sheriff!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.sheriff["per_game"]}"),
                        "don" to mapOf("first" to "${us.stats.winRate!!.don!!.win!!.percent}%", "second" to "${us.stats.advancedPoints!!.black["per_game"]}"),
                        "header" to mapOf("first" to "${us.stats.winRate!!.totalWins!!.percent}%", "second" to "${us.stats.advancedPoints!!.points10Games}")
                )
                //gameId = game.id!!
        )

        return gamePlayer
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