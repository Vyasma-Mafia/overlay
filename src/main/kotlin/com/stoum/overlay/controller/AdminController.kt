package com.stoum.overlay.controller

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("admin")
class AdminController(
    val emitterService: EmitterService,
    val gameRepository: GameRepository,
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/games/change_game")
    fun changeGame(@RequestParam from: String, @RequestParam to: String) {
        val toGame = gameRepository.findById(UUID.fromString(to)).get()
        log.info("Changing game $from to ${toGame.id}")
        if (toGame.id != null) {
            emitterService.changeGame(from, toGame)
        }
    }

    /**
     * НОВАЯ РУЧКА: Получает все игры для заданного турнира.
     * Будет использоваться для предзаполнения списка при открытии модального окна.
     */
    @GetMapping("/tournaments/{tournamentId}/games")
    fun getTournamentGames(@PathVariable tournamentId: Int): List<GameSearchResult> {
        return gameRepository.findGamesByTournamentId(tournamentId)
            .map(this::mapGameToSearchResult) // Используем вспомогательный метод для маппинга
            .sortedWith(compareBy({ it.gameNum }, { it.tableNum })) // Сортируем для порядка
    }

    /**
     * ОБНОВЛЕННАЯ РУЧКА ПОИСКА: Принимает фильтры по отдельным полям.
     */
    @GetMapping("/games/search")
    fun searchGames(
        @RequestParam(required = false) tournamentId: Int?,
        @RequestParam(required = false) gameNum: Int?,
        @RequestParam(required = false) tableNum: Int?
    ): List<GameSearchResult> {
        // Если все параметры null, возвращаем пустой список, чтобы не загружать все игры
        if (tournamentId == null && gameNum == null && tableNum == null) {
            return emptyList()
        }

        return gameRepository.findAll()
            .filter { game ->
                // Фильтруем по каждому предоставленному параметру
                val tournamentMatch = tournamentId == null || game.tournamentId == tournamentId
                val gameNumMatch = gameNum == null || game.gameNum == gameNum
                val tableNumMatch = tableNum == null || game.tableNum == tableNum
                tournamentMatch && gameNumMatch && tableNumMatch
            }
            .map(this::mapGameToSearchResult)
            .sortedWith(compareBy({ it.tournamentId }, { it.gameNum }, { it.tableNum }))
            .take(50) // Ограничиваем количество результатов
    }

    /**
     * Вспомогательная функция для консистентного маппинга Game -> GameSearchResult.
     */
    private fun mapGameToSearchResult(game: Game): GameSearchResult {
        return GameSearchResult(
            id = game.id!!,
            type = game.type,
            tournamentId = game.tournamentId,
            gameNum = game.gameNum,
            tableNum = game.tableNum,
            phase = game.phase,
            text = game.text,
            displayText = "Турнир ${game.tournamentId ?: '?'} / " +
                "Игра ${game.gameNum ?: '?'} / " +
                "Стол ${game.tableNum ?: '?'}"
        )
    }

    data class GameSearchResult(
        val id: UUID,
        val type: GameType,
        val tournamentId: Int?,
        val gameNum: Int?,
        val tableNum: Int?,
        val phase: Int?,
        val text: String?,
        // Сгенерированное поле для отображения в списке
        val displayText: String
    )
}
