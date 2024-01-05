package com.stoum.overlay.service

import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Service
class EmitterService {

    val emitters = ConcurrentHashMap<String, SseEmitter>()

    fun registerEmitter(id: String, emitter: SseEmitter): SseEmitter {
        emitters[id] = emitter
        return emitter
    }
}