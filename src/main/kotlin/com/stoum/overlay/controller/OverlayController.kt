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

// Определение enum для типов страниц
enum class PageType {
    OVERLAY, CONTROL, ROLESELECTOR;

    // Метод для получения пути в URL
    fun getPathValue(): String {
        return when (this) {
            OVERLAY -> "overlay"
            CONTROL -> "control"
            ROLESELECTOR -> "roleselector"
        }
    }

    companion object {
        // Метод для получения PageType из строки пути
        fun fromPathValue(value: String): PageType {
            return entries.find { it.name.lowercase() == value.lowercase() }
                ?: throw IllegalArgumentException("Unknown page type: $value")
        }
    }
}

// Конвертер для PageType
@Component
class PageTypeConverter : Converter<String, PageType> {
    override fun convert(source: String): PageType {
        return PageType.fromPathValue(source)
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

        val nextGame = findNextGame(game.get().type, tournamentId, gameNum, tableNum, phase)

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

    @RequestMapping("/{service}/tournaments/{tournamentId}/{pageType}")
    fun redirectToFirstGamePage(
        @PathVariable service: ServiceType,
        @PathVariable tournamentId: Int,
        @PathVariable pageType: PageType,
        model: Model,
    ): String {
        val game = gameRepository.findFirstByTournamentIdAndResultIsNullOrderByPhaseAscGameNumAsc(tournamentId)
            ?: return "game-not-found"

        val phase = game.phase
        val tableNum = game.tableNum
        val gameNum = game.gameNum

        return "redirect:/${service.getPathValue()}/tournaments/$tournamentId/phases/$phase/tables/$tableNum/games/$gameNum/${pageType.getPathValue()}"
    }

    @RequestMapping("/{service}/tournaments/{tournamentId}/tables/{tableNum}/{pageType}")
    fun redirectToFirstGamePageForTable(
        @PathVariable service: ServiceType,
        @PathVariable tournamentId: Int,
        @PathVariable tableNum: Int,
        @PathVariable pageType: PageType,
        model: Model,
    ): String {
        val game = gameRepository.findFirstByTournamentIdAndTableNumAndResultIsNullOrderByPhaseAscGameNumAsc(
            tournamentId,
            tableNum
        )
            ?: return "game-not-found"

        val phase = game.phase
        val gameNum = game.gameNum

        return "redirect:/${service.getPathValue()}/tournaments/$tournamentId/phases/$phase/tables/$tableNum/games/$gameNum/${pageType.getPathValue()}"
    }

    @RequestMapping("/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/roleselector")
    fun roleSelector(
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

        // Передаем данные игроков напрямую в модель
        if (game != null) {
            model.addAttribute("gameTitle", game.text ?: "Игра $gameNum")
            model.addAttribute("players", game.players.sortedBy { it.place })
        } else {
            model.addAttribute("gameTitle", "Игра $gameNum")
            model.addAttribute("players", emptyList<Any>())
        }

        getLogger().info("Role selector for ${game?.id}: ${service.name}, $tournamentId, $gameNum, $tableNum")

        return "role-selector-game"
    }

    @PostMapping("/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/nextgame")
    @ResponseBody
    fun nextGameFromRoleSelector(
        @PathVariable service: ServiceType,
        @PathVariable tournamentId: Int,
        @PathVariable phase: Int,
        @PathVariable tableNum: Int,
        @PathVariable gameNum: Int
    ): Map<String, Any> {
        getLogger().info("Получен запрос на переход к следующей игре: service=${service.name}, tournamentId=$tournamentId, phase=$phase, tableNum=$tableNum, gameNum=$gameNum")

        try {
            val gameType = GameType.valueOf(service.name)
            getLogger().info("Определен тип игры: $gameType")

            val nextGame = findNextGame(gameType, tournamentId, gameNum, tableNum, phase)
            getLogger().info("Результат поиска следующей игры: $nextGame")

            if (nextGame != null) {
                getLogger().info("Переход к следующей игре: ${service.name}, $tournamentId, ${nextGame.gameNum}, $tableNum, $phase")

                // Формируем URL для перенаправления
                val nextGameUrl =
                    "/${service.getPathValue()}/tournaments/$tournamentId/phases/$phase/tables/$tableNum/games/${nextGame.gameNum}/roleselector"
                getLogger().info("Сформирован URL для перенаправления: $nextGameUrl")

                val response = mapOf(
                    "success" to true,
                    "message" to "Переход к игре ${nextGame.gameNum} выполнен успешно",
                    "redirectUrl" to nextGameUrl,
                    "gameNum" to nextGame.gameNum.toString()
                )
                getLogger().info("Отправляем успешный ответ: $response")
                return response
            } else {
                val response = mapOf(
                    "success" to false,
                    "message" to "Следующая игра не найдена или не может быть создана"
                )
                getLogger().warn("Следующая игра не найдена, отправляем ответ: $response")
                return response
            }
        } catch (e: Exception) {
            getLogger().error(
                "Ошибка при переходе к следующей игре: service=${service.name}, tournamentId=$tournamentId, phase=$phase, tableNum=$tableNum, gameNum=$gameNum",
                e
            )
            val response = mapOf(
                "success" to false,
                "message" to "Ошибка при переходе к следующей игре: ${e.message}"
            )
            getLogger().error("Отправляем ответ с ошибкой: $response")
            return response
        }
    }

    /**
     * Общий метод для поиска следующей игры
     */
    private fun findNextGame(gameType: GameType, tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        return when (gameType) {
            GameType.POLEMICA -> polemicaService.getNextGame(
                PolemicaService.PolemicaTournamentGame(tournamentId, gameNum, tableNum, phase)
            )

            GameType.GOMAFIA -> gomafiaService.getGame(tournamentId, gameNum + 1, tableNum)
            GameType.CUSTOM -> null
        }
    }

    private fun getOrCreatePolemicaGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        return polemicaService.getOrTryCreateGame(tournamentId, gameNum, tableNum, phase)
    }

    private fun getOrCreateGomafiaGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game? {
        return gomafiaService.getGame(tournamentId, gameNum, tableNum)
    }

    @PostMapping("/{id}/clearRoles")
    @ResponseBody
    fun clearRoles(@PathVariable id: String) {
        val gameId = UUID.fromString(id)
        polemicaService.clearRoles(gameId)
    }

    @PostMapping("/{id}/updateRoles")
    @ResponseBody
    fun updateRoles(
        @PathVariable id: String,
        @org.springframework.web.bind.annotation.RequestBody roles: Map<Int, String>
    ) {
        val gameId = UUID.fromString(id)
        polemicaService.updateRoles(gameId, roles)
    }
}
