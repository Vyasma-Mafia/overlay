package com.stoum.overlay.controller

import com.google.gson.Gson
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.Player
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

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

        emitterService.emitGame(id)
    }

    @PostMapping("/{id}/status")
    fun setStatus(@PathVariable id: String, @RequestBody status: Map<String, List<String>>) {
        val game = gameRepository.findById(UUID.fromString(id)).get()

        status.forEach { (nickname, s) ->
            game.getPlayerByNickname(nickname).status = s[1] to s[0]
        }

        gameRepository.save(game)

        emitterService.emitGame(id)
    }

    fun Game.getPlayerByPlace(place: Int): Player {
        return this.players.find { p -> p.place == place - 1 }!!
    }

    fun Game.getPlayerByNickname(name: String): Player {
        return this.players.find { p -> p.nickname == name }!!
    }
}