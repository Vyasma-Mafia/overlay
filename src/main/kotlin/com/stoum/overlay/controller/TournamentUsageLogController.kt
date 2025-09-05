package com.stoum.overlay.controller

import com.stoum.overlay.service.TournamentUsageLogService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TournamentUsageLogController(
    private val tournamentUsageLogService: TournamentUsageLogService
) {

    @GetMapping("/admin/tournament-usage")
    fun showTournamentUsage(model: Model): String {
        val stats = tournamentUsageLogService.getTournamentUsageStats()
        model.addAttribute("stats", stats)
        return "admin/tournament_usage"
    }
}