package com.stoum.overlay.controller

import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType
import com.stoum.overlay.repository.PlayerRepository
import com.stoum.overlay.service.PhotoMigrationService
import com.stoum.overlay.service.PlayerPhotoService
import com.stoum.overlay.service.PlayerService
import com.stoum.overlay.service.TournamentOverlayService
import com.stoum.overlay.service.TournamentService
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Controller
@RequestMapping("/admin/photos")
class PhotoAdminController(
    private val tournamentService: TournamentService,
    private val playerService: PlayerService,
    private val playerRepository: PlayerRepository,
    private val playerPhotoService: PlayerPhotoService,
    private val tournamentOverlayService: TournamentOverlayService,
    private val photoMigrationService: PhotoMigrationService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    // Страница 1: Список турниров
    @GetMapping("/tournaments")
    fun showTournamentsPage(
        @RequestParam(defaultValue = "POLEMICA") source: GameType,
        @RequestParam(required = false) query: String?,
        model: Model
    ): String {
        val tournaments = runBlocking { tournamentService.getTournaments(source) }
        val filteredTournaments = if (query.isNullOrBlank()) {
            tournaments
        } else {
            tournaments.filter { it.title.contains(query, ignoreCase = true) }
        }

        model.addAttribute("tournaments", filteredTournaments)
        model.addAttribute("currentSource", source)
        model.addAttribute("allSources", GameType.entries.toTypedArray())
        model.addAttribute("query", query ?: "")
        return "admin/tournaments" // путь к html файлу
    }

    // Страница 2: Участники турнира
    @GetMapping("/tournaments/{source}/{tournamentId}")
    fun showTournamentParticipantsPage(
        @PathVariable source: GameType,
        @PathVariable tournamentId: String,
        model: Model
    ): String {
        val participants = runBlocking { tournamentService.getTournamentParticipants(source, tournamentId) }

        model.addAttribute("participants", participants)
        model.addAttribute("source", source.name)
        model.addAttribute("tournamentId", tournamentId)
        // Тут можно добавить получение названия турнира для заголовка
        // model.addAttribute("tournamentTitle", "Название турнира")

        return "admin/participants" // путь к html файлу
    }

    // Страница 3: Карточка игрока
    @GetMapping("/players/{playerId}")
    fun showPlayerCardPage(@PathVariable playerId: UUID, model: Model): String {
        val viewData = runBlocking {
            playerService.getPlayerCardDetails(playerId)
        }
        model.addAttribute("view", viewData)
        return "admin/player_card"
    }

    // Страница поиска игроков
    @GetMapping("/players/search")
    fun showPlayerSearchPage(
        @RequestParam(required = false) query: String?,
        model: Model
    ): String {
        val players = if (!query.isNullOrBlank()) {
            playerService.searchPlayers(query)
        } else {
            emptyList()
        }

        model.addAttribute("players", players)
        model.addAttribute("query", query ?: "")
        return "admin/player_search"
    }

    // API для обновления ID игрока с проверкой дубликатов
    @PostMapping("/players/{playerId}/ids")
    @ResponseBody
    fun updatePlayerIds(
        @PathVariable playerId: UUID,
        @RequestParam(required = false) polemicaId: Long?,
        @RequestParam(required = false) gomafiaId: Long?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Проверяем на дубликаты
            val duplicateCheck = playerService.checkForDuplicateIds(playerId, polemicaId, gomafiaId)

            if (duplicateCheck.hasDuplicates) {
                // Возвращаем информацию о дубликатах
                val response = mutableMapOf<String, Any>(
                    "status" to "duplicate_found",
                    "hasDuplicates" to true
                )

                duplicateCheck.duplicatePolemicaPlayer?.let { player ->
                    response["duplicatePolemicaPlayer"] = mapOf(
                        "id" to player.id,
                        "nickname" to player.nickname,
                        "polemicaId" to player.polemicaId,
                        "gomafiaId" to player.gomafiaId
                    )
                }

                duplicateCheck.duplicateGomafiaPlayer?.let { player ->
                    response["duplicateGomafiaPlayer"] = mapOf(
                        "id" to player.id,
                        "nickname" to player.nickname,
                        "polemicaId" to player.polemicaId,
                        "gomafiaId" to player.gomafiaId
                    )
                }

                ResponseEntity.ok(response)
            } else {
                // Нет дубликатов, обновляем
                val updatedPlayer = playerService.updatePlayerIds(playerId, polemicaId, gomafiaId)
                ResponseEntity.ok(
                    mapOf(
                        "status" to "success",
                        "player" to mapOf(
                            "id" to updatedPlayer.id,
                            "nickname" to updatedPlayer.nickname,
                            "polemicaId" to updatedPlayer.polemicaId,
                            "gomafiaId" to updatedPlayer.gomafiaId
                        )
                    )
                )
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to e.message.toString()
                )
            )
        }
    }

    // API для объединения профилей
    @PostMapping("/players/{primaryId}/merge/{secondaryId}")
    @ResponseBody
    fun mergePlayers(
        @PathVariable primaryId: UUID,
        @PathVariable secondaryId: UUID
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val mergedPlayer = playerService.mergePlayers(primaryId, secondaryId)
            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "player" to mapOf(
                        "id" to mergedPlayer.id,
                        "nickname" to mergedPlayer.nickname,
                        "polemicaId" to mergedPlayer.polemicaId,
                        "gomafiaId" to mergedPlayer.gomafiaId
                    )
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to e.message.toString()
                )
            )
        }
    }

    @PostMapping("/players")
    fun addPlayer(
        @RequestParam nickname: String,
        @RequestParam polemicaId: Long?,
        @RequestParam gomafiaId: Long?
    ): Player {
        return playerRepository.save(Player(nickname = nickname, polemicaId = polemicaId, gomafiaId = gomafiaId))
    }

    @PostMapping(
        "/players/{playerId}/photos",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun addPlayerPhoto(
        @PathVariable("playerId") playerId: UUID,
        @RequestPart(value = "photoType", required = false) photoType: String?,
        @RequestPart(value = "tournamentType", required = false) tournamentType: String?,
        @RequestPart(value = "tournamentId", required = false) tournamentId: String?,
        @RequestPart(value = "saveAsMain", required = false) saveAsMain: String?,
        @RequestPart("photo") photoFile: MultipartFile
    ): ResponseEntity<Player> {
        // Проверка на пустой файл
        if (photoFile.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        // Делегируем всю логику сервису
        try {
            val updatedPlayer = runBlocking {
                playerPhotoService.addPlayerPhoto(
                    playerId,
                    photoFile,
                    photoType?.let { PhotoType.valueOf(it) },
                    tournamentType?.let { GameType.valueOf(it) },
                    tournamentId?.toLongOrNull(),
                    saveAsMain?.toBoolean() ?: false
                )
            }
            return ResponseEntity.ok(updatedPlayer)
        } catch (e: PlayerPhotoService.PlayerNotFoundException) {
            return ResponseEntity.notFound().build()
        } catch (e: PlayerPhotoService.S3UploadException) {
            log.error("S3 upload exception", e)
            return ResponseEntity.status(500).body(null)
        }
    }

    @PostMapping("/tournaments/{source}/{tournamentId}/overlay-settings")
    @ResponseBody
    fun updateOverlaySettings(
        @PathVariable source: String,
        @PathVariable tournamentId: Long,
        @RequestParam password: String,
        @RequestParam enabled: Boolean
    ): ResponseEntity<Map<String, Any>> {
        try {
            val settings =
                tournamentOverlayService.toggleOverlay(GameType.valueOf(source), tournamentId, password, enabled)
            return ResponseEntity.ok().body(
                mapOf(
                    "status" to "success",
                    "gameType" to settings.gameType.name,
                    "tournamentId" to settings.tournamentId,
                    "overlayEnabled" to settings.overlayEnabled
                )
            )
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to e.message.toString()
                )
            )
        }
    }

    @GetMapping("/tournaments/{source}/{tournamentId}/overlay-settings")
    @ResponseBody
    fun getOverlaySettings(
        @PathVariable source: String,
        @PathVariable tournamentId: Long
    ): ResponseEntity<Map<String, Any>> {
        val settings = tournamentOverlayService.getSettings(GameType.valueOf(source), tournamentId)
        return ResponseEntity.ok().body(
            mapOf(
                "gameType" to settings.gameType.name,
                "tournamentId" to settings.tournamentId.toString(),
                "overlayEnabled" to settings.overlayEnabled.toString(),
                "updatedAt" to settings.updatedAt.toString()
            )
        )
    }

    @PostMapping("/migrate")
    @ResponseBody
    fun migratePhotos(): ResponseEntity<Map<String, Any>> {
        return try {
            log.info("Starting photo migration...")
            val result = runBlocking {
                photoMigrationService.migratePhotos()
            }
            ResponseEntity.ok().body(
                mapOf(
                    "status" to "success",
                    "totalPhotos" to result.totalPhotos,
                    "migrated" to result.migrated,
                    "skipped" to result.skipped,
                    "errors" to result.errors,
                    "errorDetails" to result.errorDetails
                )
            )
        } catch (e: Exception) {
            log.error("Photo migration failed", e)
            ResponseEntity.status(500).body(
                mapOf(
                    "status" to "error",
                    "message" to e.message.toString()
                )
            )
        }
    }
}

