package com.stoum.overlay.controller

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.StageType
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.polemica.PolemicaService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@RestController
class RoleSelectorController(
    val gameRepository: GameRepository,
    val polemicaService: PolemicaService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/{id}/exportToPolemica")
    fun exportToPolemica(@PathVariable id: String, @RequestBody roles: Map<Int, String>): ResponseEntity<String> {
        try {
            val game = gameRepository.findById(UUID.fromString(id)).getOrNull()
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Game not found")

            // Проверяем, что это игра Polemica
            if (game.type != com.stoum.overlay.entity.enums.GameType.POLEMICA) {
                return ResponseEntity.badRequest().body("Export to Polemica is only available for Polemica games")
            }

            // Обновляем роли игроков
            roles.forEach { (playerPlace, role) ->
                val player = game.players.find { it.place == playerPlace }
                if (player != null) {
                    player.role = role
                }
            }

            // Сохраняем обновленную игру
            gameRepository.save(game)

            val (tournamentId, gameNum, tableNum, phase) = with(game) {
                listOfNotNull(tournamentId, gameNum, tableNum, phase)
                    .takeIf { it.size == 4 } ?: return ResponseEntity.badRequest().body("Invalid game data")
            }

            val polemicaOriginalGame = polemicaService.getPolemicaGame(tournamentId, gameNum, tableNum, phase)
                ?: return ResponseEntity.badRequest().body("Polemica game not found")

            if (polemicaOriginalGame.result != null || polemicaOriginalGame.stage?.type != StageType.DEALING) {
                return ResponseEntity.badRequest().body("Polemica game is already started")
            }

            // Создаем PolemicaGame для экспорта
            val polemicaPlayers = polemicaOriginalGame.players?.map {
                PolemicaPlayer(
                    position = it.position,
                    username = it.username,
                    role = polemicaService.roleToPolemicaRole(roles[it.position.value]),
                    techs = it.techs,
                    fouls = it.fouls,
                    guess = it.guess,
                    player = it.player,
                    disqual = it.disqual,
                    award = it.award
                )
            }

            val polemicaGame = with(polemicaOriginalGame) {

                PolemicaGame(
                    id = polemicaOriginalGame.id,
                    master = master, // Будет заполнено сервисом
                    referee = referee, // Будет заполнено сервисом
                    scoringVersion = scoringVersion,
                    scoringType = scoringType,
                    version = version,
                    zeroVoting = zeroVoting,
                    tags = tags,
                    players = polemicaPlayers,
                    checks = checks,
                    shots = shots,
                    stage = stage,
                    votes = votes,
                    comKiller = comKiller,
                    bonuses = bonuses,
                    started = started,
                    stop = stop,
                    isLive = true,
                    result = result,
                    num = num,
                    table = table,
                    phase = phase,
                    factor = factor
                )
            }

            // Экспортируем в Polemica
            polemicaService.polemicaClient.postGameToCompetition(game.tournamentId!!.toLong(), polemicaGame)

            log.info("Successfully exported roles to Polemica for game ${game.id}")
            return ResponseEntity.ok("Roles successfully exported to Polemica")
        } catch (e: Exception) {
            log.error("Error exporting roles to Polemica for game $id", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error exporting roles: ${e.message}")
        }
    }
}