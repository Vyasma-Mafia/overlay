package com.stoum.overlay.controller

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.StageType
import com.google.gson.Gson
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.overlay.GamePlayer
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.PlayerPhotoService
import com.stoum.overlay.service.polemica.PolemicaService
import org.hibernate.validator.constraints.Range
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@RestController
class GameController(
    val emitterService: EmitterService,
    val gameRepository: GameRepository,
    val photoService: PlayerPhotoService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    // --- Внутренний хелпер для уменьшения дублирования кода ---
    private fun findGameAndDo(id: String, action: (Game) -> Unit): ResponseEntity<Void> {
        val game = gameRepository.findById(UUID.fromString(id)).getOrNull()
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        action(game)
        gameRepository.save(game)
        emitterService.emitGame(game)
        return ResponseEntity.ok().build() // Явный возврат 200 OK
    }

// --- Существующие эндпоинты, адаптированные под хелпер (если применимо) ---

    @PostMapping("/{id}/game")
    fun update(@PathVariable id: String, @RequestBody gameInfo: GameInfo): ResponseEntity<Void> {
        val gameJson = Gson().toJson(gameInfo)
        log.info("Updating game $id with $gameJson (raw JSON provided by client)")
        // Этот эндпоинт особенный, он принимает GameInfo и просто пересылает его.
        // Возможно, его стоит пересмотреть, если GameInfo - это полная структура игры.
        // Если GameInfo это частичное обновление, логика должна быть сложнее.
        // Пока оставляем как есть, но findGameAndDo тут не очень подходит.
        emitterService.sendTo(id, "!gameinfo $gameJson")
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/roles")
    fun setRole(@PathVariable id: String, @RequestBody roles: Map<Int, String>): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            roles.forEach { (playerNum, role) ->
                game.getPlayerByPlace(playerNum).role = role.ifBlank { null }
            }
            updateGamePhotos(game)
        }
    }

    private fun updateGamePhotos(game: Game) {
        val tournamentId = game.tournamentId ?: return
        game.players.filter { it.customPhoto != true }.forEach { player ->
            val sourcePlayerId = player.sourcePlayerId ?: return@forEach
            player.photoUrl = photoService.getPlayerPhotoUrlForPlayerCompetitionRole(
                sourcePlayerId, game.type,
                tournamentId.toLong(), player.role
            )
        }
    }


    @PostMapping("/{id}/status")
    fun setStatus(@PathVariable id: String, @RequestBody status: Map<Int, String?>): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            status.forEach { (place, s) ->
                game.getPlayerByPlace(place).status = s // null теперь корректно обрабатывается для сброса
            }
        }
    }

    @PostMapping("/{id}/setSpeaker")
    fun setSpeaker(@PathVariable id: String, @RequestParam(required = false) playerNum: Int?): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            game.players.forEach { it.speaker = false }
            playerNum?.let { game.getPlayerByPlace(it).speaker = true }
        }
    }

    @PostMapping("/{id}/visibleOverlay")
    fun setVisibleOverlay(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.visibleOverlay = value }
    }

    @PostMapping("/{id}/visibleRoles")
    fun setVisibleRoles(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.visibleRoles = value }
    }

    @PostMapping("/{id}/visibleScores")
    fun setVisibleScores(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.visibleScores = value }
    }

    @PostMapping("/{id}/started")
    fun setStarted(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.started = value }
    }

    @PostMapping("/{id}/text")
    fun setText(@PathVariable id: String, @RequestParam value: String): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.text = value }
    }

    @PostMapping("/{id}/delay")
    fun setDelay(@PathVariable id: String, @RequestParam @Range(min = 0) value: Int): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.delay = value }
    }

    @PostMapping("/{id}/autoNextGame")
    fun setAutoNextGame(@PathVariable id: String, @RequestParam value: Boolean): ResponseEntity<Void> {
        return findGameAndDo(id) { game -> game.autoNextGame = value }
    }

    @PostMapping("/{id}/resetStatuses")
    fun resetStatuses(@PathVariable id: String): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            game.players.forEach { it.status = null }
            // Сброс UI для guess (лучший ход) также должен произойти,
            // так как они часто связаны со статусом "first-killed".
            // Если это не требуется явно, можно убрать.
            game.players.forEach { it.guess = mutableListOf() }
        }
    }

    @PostMapping("/{id}/resetRoles")
    fun resetRoles(@PathVariable id: String): ResponseEntity<Void> {
        return findGameAndDo(id) { game ->
            game.players.forEach { it.role = "red" } // ИЗМЕНЕНО: Сброс на "red"
            // Сброс проверок при сбросе ролей
            game.players.forEach { it.checks = mutableListOf() }
        }
    }

// --- Новые эндпоинты для управления списками проверок и лучшими ходами ---

    /**
     * Устанавливает полный список проверок для игрока (шериф или дон).
     * Старые проверки игрока заменяются новым списком.
     */
    @PostMapping("/{id}/setPlayerChecks")
    fun setPlayerChecks(
        @PathVariable id: String,
        @RequestBody request: PlayerChecksRequest
    ): ResponseEntity<Void> {
        log.info("Setting player checks in ${id} for player ${request.playerNum} with targets: ${request.targetPlayerNums}")
        return findGameAndDo(id) { game ->
            val player = game.getPlayerByPlace(request.playerNum)
            val newChecks = mutableListOf<Map<String, String>>()

            request.targetPlayerNums.forEach { targetPlayerNum ->
                val targetPlayer = game.getPlayerByPlace(targetPlayerNum)
                // Добавляем проверку, только если у целевого игрока есть роль
                targetPlayer.role?.let { roleOfTarget ->
                    val check = mapOf(
                        "role" to roleOfTarget, // Результат проверки (цвет)
                        "num" to targetPlayerNum.toString() // Номер проверяемого
                    )
                    newChecks.add(check)
                }
            }
            player.checks = newChecks // Полностью перезаписываем список проверок
        }
    }

    /**
     * Устанавливает полный список лучших ходов для игрока (обычно первого убитого).
     */
    @PostMapping("/{id}/setPlayerGuesses")
    fun setPlayerGuesses(
        @PathVariable id: String,
        @RequestBody request: PlayerGuessesRequest
    ): ResponseEntity<Void> {
        log.info("Setting player guesses in ${id} for player ${request.playerNum} with targets: ${request.targetPlayerNums}")
        return findGameAndDo(id) { game ->
            val player = game.getPlayerByPlace(request.playerNum)
            val newGuesses = mutableListOf<Map<String, String>>()

            request.targetPlayerNums.forEach { targetPlayerNum ->
                val guess = mapOf(
                    "role" to "black", // Подразумевается, что угадывают черных
                    "num" to targetPlayerNum.toString()
                )
                newGuesses.add(guess)
            }
            player.guess = newGuesses // Полностью перезаписываем список лучших ходов
        }
    }

    // --- Вспомогательные функции для объекта Game ---
    fun Game.getPlayerByPlace(place: Int): GamePlayer {
        return this.players.find { p -> p.place == place }
            ?: throw IllegalArgumentException("Player not found at place: $place in game $this.id")
    }
}

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

data class PlayerChecksRequest(
    val playerNum: Int, // Номер игрока, который делает проверки
    val targetPlayerNums: List<Int> // Список номеров игроков, которых он проверил
)

data class PlayerGuessesRequest(
    val playerNum: Int, // Номер игрока, который делает "лучший ход"
    val targetPlayerNums: List<Int> // Список номеров игроков, которых он угадал (до 3)
)
