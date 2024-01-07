package com.stoum.overlay.controller

import com.google.gson.Gson
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.service.EmitterService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class GameController(
    val emitterService: EmitterService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/{id}/game")
    fun update(@PathVariable id: String, @RequestBody gameInfo: GameInfo) {
        val gameJson = Gson().toJson(gameInfo)
        log.info("Updating game $id with $gameJson")
        emitterService.sendTo(id, "!gameinfo $gameJson")
    }
}