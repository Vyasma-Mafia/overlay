package com.stoum.overlay.controller

import com.stoum.overlay.entity.Fact
import com.stoum.overlay.entity.FactStage
import com.stoum.overlay.model.StageOptions
import com.stoum.overlay.repository.FactRepository
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Controller
@RequestMapping("/admin/games")
class GameFactsAdminController(
    private val gameRepository: GameRepository,
    private val factRepository: FactRepository,
    private val playerPhotoService: PlayerPhotoService,
    private val emitterService: EmitterService
) {
    private val taskExecutorService = Executors.newScheduledThreadPool(1)

    @GetMapping("/{gameId}/facts")
    fun getGameFacts(@PathVariable gameId: UUID, model: Model): String {
        val game = gameRepository.findById(gameId).orElse(null)
            ?: return "redirect:/admin/games"

        val facts = game.facts

        model.addAttribute("game", game)
        model.addAttribute("facts", facts)
        model.addAttribute("stageOptions", StageOptions.AVAILABLE_STAGES)

        return "admin/game_facts"
    }

    @GetMapping("/{gameId}/facts/api")
    @ResponseBody
    fun getGameFactsApi(@PathVariable gameId: UUID): ResponseEntity<List<Map<String, Any?>>> {
        val game = gameRepository.findById(gameId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val factsData = game.facts.map { fact ->
            mapOf(
                "id" to fact.id.toString(),
                "text" to fact.text,
                "playerPhotoUrl" to fact.playerPhotoUrl,
                "stageType" to fact.stage.type,
                "stageDay" to fact.stage.day,
                "stagePlayer" to fact.stage.player,
                "displayDurationSeconds" to fact.displayDurationSeconds,
                "isDisplayed" to fact.isDisplayed
            )
        }

        return ResponseEntity.ok(factsData)
    }

    @PostMapping("/{gameId}/facts")
    @ResponseBody
    fun createFact(
        @PathVariable gameId: UUID,
        @RequestParam text: String,
        @RequestParam playerPlace: Int?,
        @RequestParam stageType: String,
        @RequestParam stageDay: Int?,
        @RequestParam stagePlayer: Int?,
        @RequestParam displayDurationSeconds: Int
    ): ResponseEntity<out Map<String, Any?>?> {
        val game = gameRepository.findById(gameId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val player = game.players.find { it.place == playerPlace }

        val fact = Fact(
            text = text,
            playerNickname = player?.nickname,
            playerPhotoUrl = player?.photoUrl,
            stage = FactStage(
                type = stageType,
                day = stageDay,
                player = stagePlayer,
                voting = null
            ),
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
                "stageType" to savedFact.stage.type,
                "stageDay" to savedFact.stage.day,
                "stagePlayer" to savedFact.stage.player,
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
        @RequestParam stageDay: Int?,
        @RequestParam stagePlayer: Int?,
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
        fact.stage = FactStage(
            type = stageType,
            day = stageDay,
            player = stagePlayer,
            voting = null
        )
        fact.displayDurationSeconds = displayDurationSeconds

        val savedFact = factRepository.save(fact)

        return ResponseEntity.ok(
            mapOf(
                "id" to savedFact.id,
                "text" to savedFact.text,
                "playerPhotoUrl" to savedFact.playerPhotoUrl,
                "stageType" to savedFact.stage.type,
                "stageDay" to savedFact.stage.day,
                "stagePlayer" to savedFact.stage.player,
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

    @PostMapping("/{gameId}/facts/{factId}/show-now")
    @ResponseBody
    fun showFactNow(
        @PathVariable gameId: UUID,
        @PathVariable factId: UUID
    ): ResponseEntity<Map<String, Any?>> {
        val game = gameRepository.findById(gameId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val fact = factRepository.findById(factId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Verify fact belongs to this game
        if (!game.facts.any { it.id == fact.id }) {
            return ResponseEntity.badRequest().build()
        }

        // Create fact data map (same structure as in PolemicaService.scheduleFactDisplay)
        val factData = mapOf(
            "type" to "fact",
            "text" to fact.text,
            "playerPhotoUrl" to fact.playerPhotoUrl,
            "playerNickname" to fact.playerNickname,
            "displayDurationSeconds" to fact.displayDurationSeconds
        )

        // Send fact immediately to overlay
        emitterService.emitFactToGame(game.id.toString(), factData)

        // Schedule hide event after displayDurationSeconds
        taskExecutorService.schedule(
            {
                val hideFactData = mapOf(
                    "type" to "hideFact",
                    "factId" to fact.id.toString()
                )
                emitterService.emitFactToGame(game.id.toString(), hideFactData)
            },
            fact.displayDurationSeconds.toLong(),
            TimeUnit.SECONDS
        )

        // Mark fact as displayed to prevent automatic re-display
        fact.isDisplayed = true
        factRepository.save(fact)

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "Fact sent to overlay",
                "factId" to fact.id.toString()
            )
        )
    }
}
