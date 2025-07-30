package com.stoum.overlay.controller

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.TournamentOverlayService
import com.stoum.overlay.service.gomafia.GomafiaService
import com.stoum.overlay.service.polemica.PolemicaService
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.Locale
import java.util.UUID

// Определение enum для типов сервисов
enum class ServiceType {
    POLEMICA, GOMAFIA;

    // Метод для получения пути в URL
    fun getPathValue(): String {
        return name.lowercase(Locale.getDefault())
    }

    companion object {
        // Метод для получения ServiceType из строки пути
        fun fromPathValue(value: String): ServiceType {
            return entries.find { it.getPathValue() == value.lowercase() }
                ?: throw IllegalArgumentException("Unknown service type: $value")
        }
    }
}

// Конвертер для параметров пути
@Component
class ServiceTypeConverter : Converter<String, ServiceType> {
    override fun convert(source: String): ServiceType {
        return ServiceType.fromPathValue(source)
    }
}

@Controller
class OverlayController(
    val gameRepository: GameRepository,
    val emitterService: EmitterService,
    val polemicaService: PolemicaService,
    val gomafiaService: GomafiaService,
    val tournamentOverlayService: TournamentOverlayService
) {
    @RequestMapping("/{id}/overlay")
    fun overlay(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "overlay"
    }

    @RequestMapping("/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/overlay")
    fun overlay(
        @PathVariable service: ServiceType,
        @PathVariable tournamentId: Int,
        @PathVariable phase: Int,
        @PathVariable tableNum: Int,
        @PathVariable gameNum: Int,
        model: Model,
    ): String? {
        // Check if tournament has overlay enabled
        if (!tournamentOverlayService.getSettings(
                GameType.valueOf(service.name),
                tournamentId.toLong()
            ).overlayEnabled
        ) {
            return "overlay-disabled"
        }

        val game = when (service) {
            ServiceType.POLEMICA -> getOrCreatePolemicaGame(tournamentId, gameNum, tableNum, phase)
            ServiceType.GOMAFIA -> getOrCreateGomafiaGame(tournamentId, gameNum, tableNum, phase)
        }

        model.addAttribute("id", game?.id)
        model.addAttribute("tournamentId", tournamentId)
        model.addAttribute("gameNum", gameNum)
        model.addAttribute("tableNum", tableNum)
        model.addAttribute("phase", phase)
        model.addAttribute("service", service.getPathValue())

        getLogger().info("Overlay for ${game?.id}: ${service.name}, $tournamentId, $gameNum, $tableNum")

        return "overlay"
    }

    @RequestMapping("/{id}/control")
    fun control(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "control-panel"
    }

    @RequestMapping("/roleselector")
    fun roleSelector(model: Model): String? {
        return "role-selector"
    }

    @PostMapping("/{id}/next")
    @ResponseBody
    fun next(@PathVariable id: String, model: Model) {
        val game = gameRepository.findById(UUID.fromString(id))
        val gameData = with(game.get()) {
            listOfNotNull(tournamentId, gameNum, tableNum, phase)
                .takeIf { it.size == 4 } ?: return
        }

        val (tournamentId, gameNum, tableNum, phase) = gameData

        val nextGame = when (game.get().type) {
            GameType.POLEMICA -> polemicaService.getNextGame(
                PolemicaService.PolemicaTournamentGame(tournamentId, gameNum, tableNum, phase)
            )

            GameType.GOMAFIA -> gomafiaService.getGame(tournamentId, gameNum + 1, tableNum)
            GameType.CUSTOM -> null
        }

        if (nextGame != null) {
            emitterService.changeGame(id, nextGame)
        }
        return
    }

    @RequestMapping("/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/control")
    fun control(
        @PathVariable service: ServiceType,
        @PathVariable tournamentId: Int,
        @PathVariable phase: Int,
        @PathVariable tableNum: Int,
        @PathVariable gameNum: Int,
        model: Model,
    ): String? {
        val game = when (service) {
            ServiceType.POLEMICA -> getOrCreatePolemicaGame(tournamentId, gameNum, tableNum, phase)
            ServiceType.GOMAFIA -> getOrCreateGomafiaGame(tournamentId, gameNum, tableNum, phase)
        }

        model.addAttribute("id", game?.id)
        model.addAttribute("tournamentId", tournamentId)
        model.addAttribute("gameNum", gameNum)
        model.addAttribute("tableNum", tableNum)
        model.addAttribute("phase", phase)
        model.addAttribute("service", service.getPathValue())

        getLogger().info("Control for ${game?.id}: ${service.name}, $tournamentId, $gameNum, $tableNum")
        return "control-panel"
    }

    private fun getOrCreatePolemicaGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        return polemicaService.getOrTryCreateGame(tournamentId, gameNum, tableNum, phase)
    }

    private fun getOrCreateGomafiaGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        return gomafiaService.getGame(tournamentId, gameNum, tableNum)
    }
}
