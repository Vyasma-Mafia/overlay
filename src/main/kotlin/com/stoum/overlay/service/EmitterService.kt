package com.stoum.overlay.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class EmitterService(
    val gameRepository: GameRepository
) {

    val objectMapper = ObjectMapper()

    private val emitters = ConcurrentHashMap<String, MutableList<SseEmitterInfo>>()

    fun emitGame(gameId: String) {
        val game = gameRepository.findById(UUID.fromString(gameId))
        game.ifPresent {
            it.playersOrdered = it.players.sortedBy { p -> p.place }.map { p -> p.nickname }
            sendTo(gameId, "!gameinfo ${objectMapper.writeValueAsString(it)}")
        }
    }

    fun registerEmitter(id: String, emitter: SseEmitter): SseEmitter {
        emitters.computeIfAbsent(id) { arrayListOf() }
        emitters[id]?.add(SseEmitterInfo(emitter, LocalDateTime.now()))
        return emitter
    }

    fun sendTo(id: String, payload: String) {
        runBlocking {
            emitters[id]?.forEach {
                launch {
                    try {
                        it.sseEmitter.send(payload)
                    } catch (e: Exception) {
                        getLogger().warn("Error on send to $id with emitter $it")
                    }
                }
            }
        }
        clearEmitters(id)
    }

    fun clearEmitters(id: String) {
        emitters[id]?.removeIf {
            try {
                it.sseEmitter.send("ping")
            } catch (e: Exception) {
                if (it.registered.isBefore(LocalDateTime.now().minusDays(1))) {
                    getLogger().info("Emitter $it for $id is deleted")
                    return@removeIf true
                }
            }
            return@removeIf false
        }
    }

    fun sendToAll(payload: String) {
        emitters.flatMap { it.value }.forEach {
            getLogger().info("sending to $it")
            it.sseEmitter.send(payload)
        }
    }

    fun hasEmitters(): Boolean {
        return emitters.isNotEmpty()
    }

    data class SseEmitterInfo(val sseEmitter: SseEmitter, val registered: LocalDateTime)
}
