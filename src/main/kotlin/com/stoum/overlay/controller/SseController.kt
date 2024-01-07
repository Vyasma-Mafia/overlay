package com.stoum.overlay.controller

import com.google.gson.Gson
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event
import java.util.*
import java.util.logging.Logger


@Controller
class SseController(
        val emitterService: EmitterService,
        val gameRepository: GameRepository
) {
    @GetMapping("/{id}/gameinfo")
    fun gameinfo(@PathVariable id: String?): SseEmitter? {
        Logger.getAnonymousLogger().info("got overlay request for sse")
        if (id != null) {
            val emitter = SseEmitter()
            emitterService.registerEmitter(id, emitter)

            emitter.send(
                event()
                    .name("message")
                    .reconnectTime(5000L)
                    .data("Registered with id $id")
            )
            val game = gameRepository.findById(UUID.fromString(id))
            return emitter.also {
                runBlocking {
                    launch {
                        delay(1000L)
                        game.ifPresent {
                            emitterService.sendTo(id, "!gameinfo ${Gson().toJson(GameInfo(it))}")
                        }
                    }
                }
            }
        }
        return null
    }

    @GetMapping("/{id}/controlinfo")
    fun control(@PathVariable id: String?): SseEmitter? {
        Logger.getAnonymousLogger().info("got control request for sse")
        if (id != null) {
            val emitter = SseEmitter()

            emitter.send(
                event()
                    .name("message")
                    .reconnectTime(5000L)
                    .data("Registered control")
            )
            val game = gameRepository.findById(UUID.fromString(id))
            return emitter.also {
                runBlocking {
                    launch {
                        delay(1000L)
                        game.ifPresent {
                            emitterService.sendTo(id, "!gameinfo ${Gson().toJson(GameInfo(it))}")
                        }
                    }
                }
            }
        }
        return null
    }
}