package com.stoum.overlay.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.stoum.overlay.repository.GameRepository
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

    val objectMapper = ObjectMapper()

    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    fun emitGame(gameId: String) {
        val game = gameRepository.findById(UUID.fromString(gameId))
        game.ifPresent {
            it.playersOrdered = it.players.sortedBy { p -> p.place }.map { p -> p.nickname }
            sendTo(gameId, "!gameinfo ${objectMapper.writeValueAsString(it)}")
        }
    }

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