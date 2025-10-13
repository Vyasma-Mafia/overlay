package com.stoum.overlay.controller

import com.github.mafia.vyasma.polemica.library.model.game.StageType
import com.stoum.overlay.entity.Fact
import com.stoum.overlay.repository.FactRepository
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.PlayerPhotoService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.UUID

@Controller
@RequestMapping("/admin/games")
class GameFactsAdminController(
    private val gameRepository: GameRepository,
    private val factRepository: FactRepository,
    private val playerPhotoService: PlayerPhotoService
) {

    @GetMapping("/{gameId}/facts")
    fun getGameFacts(@PathVariable gameId: UUID, model: Model): String {
        val game = gameRepository.findById(gameId).orElse(null)
            ?: return "redirect:/admin/games"

        val facts = game.facts

        model.addAttribute("game", game)
        model.addAttribute("facts", facts)
        model.addAttribute("stageTypes", StageType.entries.toTypedArray())

        return "admin/game_facts"
    }

    @PostMapping("/{gameId}/facts")
    @ResponseBody
    fun createFact(
        @PathVariable gameId: UUID,
        @RequestParam text: String,
        @RequestParam playerPlace: Int?,
        @RequestParam stageType: String,
        @RequestParam displayDurationSeconds: Int
    ): ResponseEntity<out Map<String, Any?>?> {
        val game = gameRepository.findById(gameId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val player = game.players.find { it.place == playerPlace }

        val fact = Fact(
            text = text,
            playerNickname = player?.nickname,
            playerPhotoUrl = player?.photoUrl,
            stageType = stageType,
            displayDurationSeconds = displayDurationSeconds,
            isDisplayed = false
        )

        game.facts.add(fact)
        val savedFact = gameRepository.save(game).facts.find { it.text == fact.text }
        if (savedFact == null) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(
            mapOf(
                "id" to savedFact.id,
                "text" to savedFact.text,
                "playerPhotoUrl" to savedFact.playerPhotoUrl,
                "stageType" to savedFact.stageType,
                "displayDurationSeconds" to savedFact.displayDurationSeconds,
                "isDisplayed" to savedFact.isDisplayed
            )
        )
    }

    @PutMapping("/{gameId}/facts/{factId}")
    @ResponseBody
    fun updateFact(
        @PathVariable gameId: UUID,
        @PathVariable factId: UUID,
        @RequestParam text: String,
        @RequestParam playerPlace: Int?,
        @RequestParam stageType: String,
        @RequestParam displayDurationSeconds: Int
    ): ResponseEntity<out Map<String, Any?>?> {
        val game = gameRepository.findById(gameId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val fact = factRepository.findById(factId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Получаем фото игрока, если указан
        val playerPhotoUrl = if (playerPlace != null) {
            val player = game.players.find { it.place == playerPlace }
            player?.photoUrl
        } else null

        fact.text = text
        fact.playerPhotoUrl = playerPhotoUrl
        fact.stageType = stageType
        fact.displayDurationSeconds = displayDurationSeconds

        val savedFact = factRepository.save(fact)

        return ResponseEntity.ok(
            mapOf(
                "id" to savedFact.id,
                "text" to savedFact.text,
                "playerPhotoUrl" to savedFact.playerPhotoUrl,
                "stageType" to savedFact.stageType,
                "displayDurationSeconds" to savedFact.displayDurationSeconds,
                "isDisplayed" to savedFact.isDisplayed
            )
        )
    }

    @DeleteMapping("/{gameId}/facts/{factId}")
    @ResponseBody
    fun deleteFact(
        @PathVariable gameId: UUID,
        @PathVariable factId: UUID
    ): ResponseEntity<Void> {
        val fact = factRepository.findById(factId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        factRepository.delete(fact)
        return ResponseEntity.ok().build()
    }
}
