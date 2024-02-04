package com.stoum.overlay.controller

import com.stoum.overlay.GomafiaRestClient
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.GameType
import com.stoum.overlay.entity.Player
import com.stoum.overlay.model.gomafia.GameDto
import com.stoum.overlay.model.gomafia.UserWithStats
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import java.util.*
import java.util.logging.Logger


@Controller
class OverlayController(
        val emitterService: EmitterService,
        val gameRepository: GameRepository,
        val gomafiaRestClient: GomafiaRestClient
) {
    @RequestMapping("/{id}/overlay")
    fun overlay(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "overlay"
    }

    @RequestMapping("/{tournamentId}/{gameNum}/{tableNum}/overlay")
    fun overlay(
            @PathVariable tournamentId: Int,
            @PathVariable gameNum: Int,
            @PathVariable tableNum: Int,
            model: Model,
    ): String? {
        val game = getOrCreateGame(tournamentId, gameNum, tableNum)

        model.addAttribute("id", game.id)

        Logger.getAnonymousLogger().info("${game.id}")

        return "overlay"
    }

    @RequestMapping("/{id}/control")
    fun control(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "control-panel"
    }

    @RequestMapping("/{tournamentId}/{gameNum}/{tableNum}/control")
    fun control(
            @PathVariable tournamentId: Int,
            @PathVariable gameNum: Int,
            @PathVariable tableNum: Int,
            model: Model,
    ): String? {
        val game = getOrCreateGame(tournamentId, gameNum, tableNum)

        model.addAttribute("id", game.id)

        Logger.getAnonymousLogger().info("${game.id}")
        return "control-panel"
    }

    private fun userWithStatsToPlayer(us: UserWithStats, game: GameDto): Player {
        val player = Player(
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

        return player
    }

    private fun getOrCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int): Game {
        var game = gameRepository.findGameByTournamentIdAndGameNumAndTableNum(tournamentId, gameNum, tableNum)
        if (game == null) {
            val tournament = gomafiaRestClient.getTournament(tournamentId)
            val gameDto = tournament.games.first { g -> g.gameNum == gameNum && g.tableNum == tableNum }
            val users = gameDto.table.map { tp -> gomafiaRestClient.getUserWithStats(tp.id!!) }
            game = Game(type = GameType.FSM, tournamentId = tournamentId, gameNum = gameNum, tableNum = tableNum)

            game.players.addAll(users.map { u -> userWithStatsToPlayer(u, gameDto) })

            game = gameRepository.save(game)
        }

        return game
    }

}
