package com.stoum.overlay.controller

import com.google.gson.Gson
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import org.hibernate.validator.constraints.Range
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class GameController(
        val emitterService: EmitterService,
        val gameRepository: GameRepository,
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/{id}/game")
    fun update(@PathVariable id: String, @RequestBody gameInfo: GameInfo) {
        val gameJson = Gson().toJson(gameInfo)
        log.info("Updating game $id with $gameJson")
        emitterService.sendTo(id, "!gameinfo $gameJson")
    }

    @PostMapping("/{id}/roles")
    fun setRole(@PathVariable id: String, @RequestBody roles: Map<Int, String>) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        roles.forEach { (playerNum, role) ->
            game.getPlayerByPlace(playerNum).role = role
        }

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/status")
    fun setStatus(@PathVariable id: String, @RequestBody status: Map<Int, String>) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        status.forEach { (place, s) ->
            game.getPlayerByPlace(place).status = s
        }

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/setSpeaker")
    fun setSpeaker(@PathVariable id: String, @RequestParam(required = false) playerNum: Int?) {
        val game = gameRepository.findById(UUID.fromString(id)).get()
        game.players.forEach { it.speaker = false }
        playerNum?.let { game.getPlayerByPlace(it).speaker = true }

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/visibleOverlay")
    fun setVisibleOverlay(@PathVariable id: String, @RequestParam value: Boolean) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        game.visibleOverlay = value

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/visibleRoles")
    fun setVisibleRoles(@PathVariable id: String, @RequestParam value: Boolean) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        game.visibleRoles = value

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/started")
    fun setStarted(@PathVariable id: String, @RequestParam value: Boolean) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        game.started = value

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/text")
    fun setText(@PathVariable id: String, @RequestParam value: String) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        game.text = value

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/resetStatuses")
    fun resetStatuses(@PathVariable id: String) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        game.players.forEach { it.status = null }

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/resetRoles")
    fun resetRoles(@PathVariable id: String) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        game.players.forEach { it.role = null }

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    /**
     * Добавляет проверку для игрока (шериф или дон)
     */
    @PostMapping("/{id}/playerCheck")
    fun playerCheck(
        @PathVariable id: String,
        @RequestBody request: CheckRequest
    ) {
        log.info("Adding player check: player ${request.playerNum} checks player ${request.targetPlayerNum}")

        val game = gameRepository.findById(UUID.fromString(id)).orElseThrow {
            IllegalArgumentException("Game not found: $id")
        }

        // Найти игрока по номеру места
        val player = game.getPlayerByPlace(request.playerNum)

        // Найти целевого игрока по номеру места
        val targetPlayer = game.getPlayerByPlace(request.targetPlayerNum)

        // Инициализировать список проверок, если он еще не существует
        if (player.checks == null) {
            player.checks = mutableListOf()
        }

        // Определить результат проверки через сервис (красный или черный)
        targetPlayer.role?.let {
            // Добавить новую проверку
            val check = mapOf(
                "role" to it,
                "num" to request.targetPlayerNum.toString()
            )

            player.checks!!.add(check)
        }

        // Сохранить игру и отправить обновление
        gameRepository.save(game)
        emitterService.emitGame(game)
    }

    /**
     * Удаляет проверку игрока
     */
    @PostMapping("/{id}/removePlayerCheck")
    fun removePlayerCheck(
        @PathVariable id: String,
        @RequestBody request: RemoveCheckRequest
    ) {
        log.info("Removing player check: player ${request.playerNum}, index ${request.checkIndex}")

        val game = gameRepository.findById(UUID.fromString(id)).orElseThrow {
            IllegalArgumentException("Game not found: $id")
        }

        // Найти игрока по номеру места
        val player = game.getPlayerByPlace(request.playerNum)

        // Проверить, что у игрока есть проверки и индекс в пределах допустимого
        if (player.checks != null && request.checkIndex >= 0 && request.checkIndex < player.checks!!.size) {
            // Удалить проверку с указанным индексом
            player.checks!!.removeAt(request.checkIndex)

            // Сохранить игру и отправить обновление
            gameRepository.save(game)
            emitterService.emitGame(game)
        } else {
            log.warn("Cannot remove check: index out of bounds or checks list is null")
        }
    }

    /**
     * Добавляет лучший ход для первого убитого игрока
     */
    @PostMapping("/{id}/playerGuess")
    fun playerGuess(
        @PathVariable id: String,
        @RequestBody request: GuessRequest
    ) {
        log.info("Adding player guess: player ${request.playerNum} guesses player ${request.targetPlayerNum}")

        val game = gameRepository.findById(UUID.fromString(id)).orElseThrow {
            IllegalArgumentException("Game not found: $id")
        }

        // Найти игрока по номеру места
        val player = game.getPlayerByPlace(request.playerNum)

        // Инициализировать список догадок, если он еще не существует
        if (player.guess == null) {
            player.guess = mutableListOf()
        }

        // Проверить, что у игрока менее 3-х догадок
        if (player.guess!!.size >= 3) {
            log.warn("Player ${request.playerNum} already has 3 guesses, cannot add more")
            return
        }

        // Добавить новую догадку
        val guess = mapOf(
            "role" to "black",
            "num" to request.targetPlayerNum.toString()
        )
        player.guess!!.add(guess)

        // Сохранить игру и отправить обновление
        gameRepository.save(game)
        emitterService.emitGame(game)
    }

    /**
     * Удаляет лучший ход игрока
     */
    @PostMapping("/{id}/removePlayerGuess")
    fun removePlayerGuess(
        @PathVariable id: String,
        @RequestBody request: RemoveGuessRequest
    ) {
        log.info("Removing player guess: player ${request.playerNum}, index ${request.guessIndex}")

        val game = gameRepository.findById(UUID.fromString(id)).orElseThrow {
            IllegalArgumentException("Game not found: $id")
        }

        // Найти игрока по номеру места
        val player = game.getPlayerByPlace(request.playerNum)

        // Проверить, что у игрока есть догадки и индекс в пределах допустимого
        if (player.guess != null && request.guessIndex >= 0 && request.guessIndex < player.guess!!.size) {
            // Удалить догадку с указанным индексом
            player.guess!!.removeAt(request.guessIndex)

            // Сохранить игру и отправить обновление
            gameRepository.save(game)
            emitterService.emitGame(game)
        } else {
            log.warn("Cannot remove guess: index out of bounds or guess list is null")
        }
    }

    @PostMapping("/{id}/delay")
    fun setVisibleRoles(@PathVariable id: String, @RequestParam @Range(min = 0) value: Int) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        game.delay = value

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    fun Game.getPlayerByPlace(place: Int): GamePlayer {
        return this.players.find { p -> p.place == place }!!
    }

    fun Game.getPlayerByNickname(name: String): GamePlayer {
        return this.players.find { p -> p.nickname == name }!!
    }
}

// Вспомогательные классы для запросов
data class CheckRequest(
    val playerNum: Int,
    val targetPlayerNum: Int
)

data class RemoveCheckRequest(
    val playerNum: Int,
    val checkIndex: Int
)

data class GuessRequest(
    val playerNum: Int,
    val targetPlayerNum: Int
)

data class RemoveGuessRequest(
    val playerNum: Int,
    val guessIndex: Int
)
