package com.stoum.overlay.controller

import com.stoum.overlay.entity.Game
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.polemica.PolemicaService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID
import java.util.logging.Logger

@Controller
class OverlayController(
    val gameRepository: GameRepository,
    val emitterService: EmitterService,
    val polemicaService: PolemicaService
) {
    @RequestMapping("/{id}/overlay")
    fun overlay(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "overlay"
    }

    @RequestMapping("/polemica/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/overlay")
    fun overlay(
        @PathVariable tournamentId: Int,
        @PathVariable phase: Int,
        @PathVariable tableNum: Int,
        @PathVariable gameNum: Int,
        model: Model,
    ): String? {
        val game = getOrCreateGame(tournamentId, gameNum, tableNum, phase)

        model.addAttribute("id", game.id)
        model.addAttribute("tournamentId", tournamentId)
        model.addAttribute("gameNum", gameNum)
        model.addAttribute("tableNum", tableNum)
        model.addAttribute("phase", phase)

        getLogger().info("Overlay for ${game.id}: $tournamentId, $gameNum, $tableNum")

        return "overlay"
    }

    @RequestMapping("/{id}/control")
    fun control(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "control-panel"
    }

    @PostMapping("/{id}/next")
    fun next(@PathVariable id: String, model: Model) {
        val game = gameRepository.findById(UUID.fromString(id))
        val (tournamentId, gameNum, tableNum, phase) = with(game.get()) {
            listOfNotNull(tournamentId, gameNum, tableNum, phase)
                .takeIf { it.size == 4 } ?: return
        }

        val nextGame =
            polemicaService.getNextGame(PolemicaService.PolemicaTournamentGame(tournamentId, gameNum, tableNum, phase))
        if (nextGame != null) {
            emitterService.changeGame(id, nextGame)
        }
    }

    @RequestMapping("/polemica/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}//control")
    fun control(
        @PathVariable tournamentId: Int,
        @PathVariable phase: Int,
        @PathVariable tableNum: Int,
        @PathVariable gameNum: Int,
        model: Model,
    ): String? {
        val game = getOrCreateGame(tournamentId, gameNum, tableNum, phase)

        model.addAttribute("id", game.id)
        model.addAttribute("tournamentId", tournamentId)
        model.addAttribute("gameNum", gameNum)
        model.addAttribute("tableNum", tableNum)
        model.addAttribute("phase", phase)

        Logger.getAnonymousLogger().info("${game.id}")
        return "control-panel"
    }

    private fun getOrCreateGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game {
        return polemicaService.getOrTryCreateGame(tournamentId, gameNum, tableNum, phase)!!
    }
}
