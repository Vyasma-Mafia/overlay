package com.stoum.overlay.controller

import com.stoum.overlay.service.EmitterService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event
import java.util.logging.Logger


@Controller
class SseController(
        val emitterService: EmitterService
) {
    @GetMapping("/{id}/gameinfo")
    fun streamEvents(@PathVariable id: String?): SseEmitter? {
        Logger.getAnonymousLogger().info("got request for sse")
        if(id != null) {
            val emitter = SseEmitter()
            emitterService.registerEmitter(id, emitter)

            emitter.send(event()
                    .name("message")
                    .reconnectTime(5000L)
                    .data("Registered with id $id")
            )
            return emitter
        }
        return null
    }
}