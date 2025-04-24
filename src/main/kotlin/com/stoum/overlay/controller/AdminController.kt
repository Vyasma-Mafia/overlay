package com.stoum.overlay.controller

import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("admin")
class AdminController(
    val emitterService: EmitterService,
    val gameRepository: GameRepository,
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/games/change_game")
    fun changeGame(@RequestParam from: String, @RequestParam to: String) {
        val toGame = gameRepository.findById(UUID.fromString(to)).get()
        log.info("Changing game $from to ${toGame.id}")
        if (toGame.id != null) {
            emitterService.changeGame(from, toGame)
        }
    }
}
