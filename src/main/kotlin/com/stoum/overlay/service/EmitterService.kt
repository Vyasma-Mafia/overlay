package com.stoum.overlay.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.stoum.overlay.entity.Game
import com.stoum.overlay.getLogger
import com.stoum.overlay.repository.GameRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong



@Service
class EmitterService(
    val gameRepository: GameRepository,
    val objectMapper: ObjectMapper
) {
    val ERRORS_TO_EXCLUDE = 3L

    private val emitters = ConcurrentHashMap<String, MutableList<SseEmitterInfo>>()

    fun emitGame(game: Game) {
        val gameId = game.id.toString()
        val gameCopy = game.copy()
        gameCopy.playersOrdered = gameCopy.players.sortedBy { p -> p.place }.map { p -> p.nickname }
        sendTo(gameId, "!gameinfo ${objectMapper.writeValueAsString(gameCopy)}")
    }

    fun emitGame(gameId: String) {
        val game = gameRepository.findById(UUID.fromString(gameId))
        game.ifPresent { emitGame(it) }
    }


    fun changeGame(gameId: String, game: Game) {
        getLogger().info("Change ${gameId} to ${game.id} (${game.type}, ${game.tournamentId}, ${game.phase}, ${game.tableNum}, ${game.gameNum})")
        sendTo(gameId, "!nextgame ${objectMapper.writeValueAsString(game)}")
    }

    fun emitFactToGame(gameId: String, data: Map<String, Any?>) {
        val payload = objectMapper.writeValueAsString(data)
        sendTo(gameId, "!fact ${payload}")
    }

    fun registerEmitter(id: String, emitter: SseEmitter): SseEmitter {
        emitters.computeIfAbsent(id) { arrayListOf() }
        getLogger().info("Registering emitter for $id: $emitter")
        emitters[id]?.add(SseEmitterInfo(emitter, AtomicLong(0)))
        gameRepository.findById(UUID.fromString(id))
            .ifPresent { game ->
                if (game.manuallyStarted != false) {
                    game.started = true
                }
                gameRepository.save(game)
            }
        return emitter
    }

    fun sendTo(id: String, payload: String) {
        runBlocking {
            emitters[id]?.forEach {
                launch {
                    try {
                        it.sseEmitter.send(payload)
                    } catch (e: Exception) {
                        getLogger().debug("Error on send to $id with emitter $it")
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
                val errors = it.errorsCounter.incrementAndGet()
                if (errors > ERRORS_TO_EXCLUDE) {
                    getLogger().info("Emitter ${it.sseEmitter} for $id is deleted on $errors")
                    it.sseEmitter.complete()
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

    fun hasEmittersForGame(id: String): Boolean {
        return emitters[id]?.isNotEmpty() == true
    }

    data class SseEmitterInfo(val sseEmitter: SseEmitter, val errorsCounter: AtomicLong)
}
