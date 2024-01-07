package com.stoum.overlay.service

import com.google.gson.Gson
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class EmitterService(
    val gameRepository: GameRepository
) {
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