package com.stoum.overlay.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Service
class EmitterService {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    fun registerEmitter(id: String, emitter: SseEmitter): SseEmitter {
        emitters[id] = emitter
        return emitter
    }

    fun sendTo(id: String, payload: String) {
        emitters[id]?.send(payload)
    }

    fun sendToAll(payload: String) {
        emitters.forEachValue(1L) {
            log.info("sending to $it")
            it.send(payload)
        }
    }

    fun hasEmitters(): Boolean {
        return emitters.isNotEmpty()
    }
}