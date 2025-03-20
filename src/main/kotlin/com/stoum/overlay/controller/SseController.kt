package com.stoum.overlay.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.stoum.overlay.getLogger
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
import java.util.UUID
import java.util.logging.Logger

@Controller
class SseController(
        val emitterService: EmitterService,
        val gameRepository: GameRepository
) {
    val objectMapper = ObjectMapper()

    @GetMapping("/{id}/gameinfo")
    fun gameinfo(@PathVariable id: String?): SseEmitter? {
        getLogger().info("got overlay request for sse $id")
        if (id != null) {
            val emitter = SseEmitter()
            emitterService.registerEmitter(id, emitter)

            emitter.send(
                event()
                    .name("message")
                    .reconnectTime(500L)
                    .data("Registered with id $id")
            )
            return emitter.also {
                runBlocking {
                    launch {
                        delay(1000L)
                        emitterService.emitGame(id)
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
                            it.playersOrdered = it.players.sortedBy { p -> p.place }.map { p -> p.nickname }
                            emitter.send(
                                event()
                                    .name("message")
                                    .reconnectTime(5000L)
                                    .data("!gameinfo ${objectMapper.writeValueAsString(it)}")
                            )
                        }
                    }
                }
            }
        }
        return null
    }
}
