package com.stoum.overlay.controller

import com.stoum.overlay.repository.GameRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/games")
class GameListAdminController(
    private val gameRepository: GameRepository
) {

    @GetMapping
    fun listGames(
        @RequestParam(required = false) tournamentId: Int?,
        model: Model
    ): String {
        val games = if (tournamentId != null) {
            gameRepository.findGamesByTournamentId(tournamentId)
        } else {
            gameRepository.findAll().take(50) // Ограничиваем количество для производительности
        }

        model.addAttribute("games", games)
        model.addAttribute("tournamentId", tournamentId)

        return "admin/games_list"
    }
}
