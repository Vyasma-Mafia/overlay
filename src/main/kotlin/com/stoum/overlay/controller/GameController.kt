package com.stoum.overlay.controller

import com.google.gson.Gson
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
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
    fun setRole(@PathVariable id: String, @RequestBody roles: Map<String, String>) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        roles.forEach { (nickname, role) ->
            game.getPlayerByNickname(nickname).role = role
        }

        gameRepository.save(game)

        emitterService.emitGame(game)
    }

    @PostMapping("/{id}/status")
    fun setStatus(@PathVariable id: String, @RequestBody status: Map<String, List<String>>) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        status.forEach { (nickname, s) ->
            game.getPlayerByNickname(nickname).status = s[1] to s[0]
        }

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


    fun Game.getPlayerByPlace(place: Int): GamePlayer {
        return this.players.find { p -> p.place == place - 1 }!!
    }

    fun Game.getPlayerByNickname(name: String): GamePlayer {
        return this.players.find { p -> p.nickname == name }!!
    }
}
