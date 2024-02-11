package com.stoum.overlay.service.gomafia

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.getLogger
import com.stoum.overlay.model.gomafia.GameDto
import com.stoum.overlay.model.gomafia.UserWithStats
import com.stoum.overlay.repository.GameRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class GomafiaService(
        val gameRepository: GameRepository,
        val gomafiaRestClient: GomafiaRestClient
) {
    val log = this.getLogger()

    @Transactional
    fun initTournament(tournamentId: Int, startWithGameNum: Int) {
        val tournament = gomafiaRestClient.getTournament(tournamentId)
        tournament.games.filter { g -> g.gameNum!! >= startWithGameNum }.forEach { gameDto ->
            log.info("processing $gameDto")
            //todo save stats once for tournament
            val users = gameDto.table.map { tp -> gomafiaRestClient.getUserWithStats(tp.id!!) }
            val game = Game(type = GameType.FSM, tournamentId = tournamentId, gameNum = gameDto.gameNum, tableNum = gameDto.tableNum)

            game.players.addAll(users.map { u -> userWithStatsToPlayer(u, gameDto) })

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

    private fun userWithStatsToPlayer(us: UserWithStats, game: GameDto): GamePlayer {
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
}