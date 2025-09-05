package com.stoum.overlay.controller

import com.google.gson.Gson
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.PlayerPhotoService
import org.hibernate.validator.constraints.Range
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@RestController
class GameController(
    val emitterService: EmitterService,
    val gameRepository: GameRepository,
    val photoService: PlayerPhotoService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    // --- Внутренний хелпер для уменьшения дублирования кода ---
    private fun findGameAndDo(id: String, action: (Game) -> Unit): ResponseEntity<Void> {
        val game = gameRepository.findById(UUID.fromString(id)).getOrNull()
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        action(game)
        gameRepository.save(game)
        emitterService.emitGame(game)
        return ResponseEntity.ok().build() // Явный возврат 200 OK
    }

// --- Существующие эндпоинты, адаптированные под хелпер (если применимо) ---

    @PostMapping("/{id}/game")
    fun update(@PathVariable id: String, @RequestBody gameInfo: GameInfo): ResponseEntity<Void> {
        val gameJson = Gson().toJson(gameInfo)
        log.info("Updating game $id with $gameJson (raw JSON provided by client)")
        // Этот эндпоинт особенный, он принимает GameInfo и просто пересылает его.
        // Возможно, его стоит пересмотреть, если GameInfo - это полная структура игры.
        // Если GameInfo это частичное обновление, логика должна быть сложнее.
        // Пока оставляем как есть, но findGameAndDo тут не очень подходит.
        emitterService.sendTo(id, "!gameinfo $gameJson")
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/roles")
    fun setRole(@PathVariable id: String, @RequestBody roles: Map<Int, String>): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            roles.forEach { (playerNum, role) ->
                game.getPlayerByPlace(playerNum).role = role.ifBlank { null }
            }
            updateGamePhotos(game)
        }
    }

    private fun updateGamePhotos(game: Game) {
        val tournamentId = game.tournamentId ?: return
        game.players.filter { it.customPhoto != true }.forEach { player ->
            val sourcePlayerId = player.sourcePlayerId ?: return@forEach
            player.photoUrl = photoService.getPlayerPhotoUrlForPlayerCompetitionRole(
                sourcePlayerId, game.type,
                tournamentId.toLong(), player.role
            )
        }
    }


    @PostMapping("/{id}/status")
    fun setStatus(@PathVariable id: String, @RequestBody status: Map<Int, String?>): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            status.forEach { (place, s) ->
                game.getPlayerByPlace(place).status = s // null теперь корректно обрабатывается для сброса
            }
        }
    }

    @PostMapping("/{id}/setSpeaker")
    fun setSpeaker(@PathVariable id: String, @RequestParam(required = false) playerNum: Int?): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            game.players.forEach { it.speaker = false }
            playerNum?.let { game.getPlayerByPlace(it).speaker = true }
        }
    }

    @PostMapping("/{id}/visibleOverlay")
    fun setVisibleOverlay(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.visibleOverlay = value }
    }

    @PostMapping("/{id}/visibleRoles")
    fun setVisibleRoles(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.visibleRoles = value }
    }

    @PostMapping("/{id}/visibleScores")
    fun setVisibleScores(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.visibleScores = value }
    }

    @PostMapping("/{id}/started")
    fun setStarted(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.started = value }
    }

    @PostMapping("/{id}/text")
    fun setText(@PathVariable id: String, @RequestParam value: String): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.text = value }
    }

    @PostMapping("/{id}/delay")
    fun setDelay(@PathVariable id: String, @RequestParam @Range(min = 0) value: Int): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.delay = value }
    }

    @PostMapping("/{id}/autoNextGame")
    fun setAutoNextGame(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.autoNextGame = value }
    }

    @PostMapping("/{id}/resetStatuses")
    fun resetStatuses(@PathVariable id: String): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            game.players.forEach { it.status = null }
            // Сброс UI для guess (лучший ход) также должен произойти,
            // так как они часто связаны со статусом "first-killed".
            // Если это не требуется явно, можно убрать.
            game.players.forEach { it.guess = mutableListOf() }
        }
    }

    @PostMapping("/{id}/resetRoles")
    fun resetRoles(@PathVariable id: String): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            game.players.forEach { it.role = "red" } // ИЗМЕНЕНО: Сброс на "red"
            // Сброс проверок при сбросе ролей
            game.players.forEach { it.checks = mutableListOf() }
        }
    }

// --- Новые эндпоинты для управления списками проверок и лучшими ходами ---

    /**
     * Устанавливает полный список проверок для игрока (шериф или дон).
     * Старые проверки игрока заменяются новым списком.
     */
    @PostMapping("/{id}/setPlayerChecks")
    fun setPlayerChecks(
        @PathVariable id: String,
        @RequestBody request: PlayerChecksRequest
    ): ResponseEntity<Void> {
        log.info("Setting player checks in ${id} for player ${request.playerNum} with targets: ${request.targetPlayerNums}")
        return findGameAndDo(id) { game ->
            val player = game.getPlayerByPlace(request.playerNum)
            val newChecks = mutableListOf<Map<String, String>>()

            request.targetPlayerNums.forEach { targetPlayerNum ->
                val targetPlayer = game.getPlayerByPlace(targetPlayerNum)
                // Добавляем проверку, только если у целевого игрока есть роль
                targetPlayer.role?.let { roleOfTarget ->
                    val check = mapOf(
                        "role" to roleOfTarget, // Результат проверки (цвет)
                        "num" to targetPlayerNum.toString() // Номер проверяемого
                    )
                    newChecks.add(check)
                }
            }
            player.checks = newChecks // Полностью перезаписываем список проверок
        }
    }

    /**
     * Устанавливает полный список лучших ходов для игрока (обычно первого убитого).
     */
    @PostMapping("/{id}/setPlayerGuesses")
    fun setPlayerGuesses(
        @PathVariable id: String,
        @RequestBody request: PlayerGuessesRequest
    ): ResponseEntity<Void> {
        log.info("Setting player guesses in ${id} for player ${request.playerNum} with targets: ${request.targetPlayerNums}")
        return findGameAndDo(id) { game ->
            val player = game.getPlayerByPlace(request.playerNum)
            val newGuesses = mutableListOf<Map<String, String>>()

            request.targetPlayerNums.forEach { targetPlayerNum ->
                val guess = mapOf(
                    "role" to "black", // Подразумевается, что угадывают черных
                    "num" to targetPlayerNum.toString()
                )
                newGuesses.add(guess)
            }
            player.guess = newGuesses // Полностью перезаписываем список лучших ходов
        }
    }

    // --- Вспомогательные функции для объекта Game ---
    fun Game.getPlayerByPlace(place: Int): GamePlayer {
        return this.players.find { p -> p.place == place }
            ?: throw IllegalArgumentException("Player not found at place: $place in game $this.id")
    }
}


data class PlayerChecksRequest(
    val playerNum: Int, // Номер игрока, который делает проверки
    val targetPlayerNums: List<Int> // Список номеров игроков, которых он проверил
)

data class PlayerGuessesRequest(
    val playerNum: Int, // Номер игрока, который делает "лучший ход"
    val targetPlayerNums: List<Int> // Список номеров игроков, которых он угадал (до 3)
)
