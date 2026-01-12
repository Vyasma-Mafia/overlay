package com.stoum.overlay.service

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType
import com.stoum.overlay.model.ParticipantView
import com.stoum.overlay.model.PlayerCardView
import com.stoum.overlay.model.PlayerTournamentPhotoView
import com.stoum.overlay.repository.GamePlayerRepository
import com.stoum.overlay.repository.PlayerRepository
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PlayerService(
    val playerRepository: PlayerRepository,
    val gamePlayerRepository: GamePlayerRepository,
    val gomafiaRestClient: GomafiaRestClient,
    val polemicaClient: PolemicaClient
) {

    // Ключевая функция: найти игрока по ID из внешних систем или по нику,
    // если не найден - создать нового.
    @Transactional
    fun findOrCreatePlayer(
        nickname: String,
        polemicaId: Long? = null,
        gomafiaId: Long? = null
    ): Player {
        val existingPlayer = when {
            polemicaId != null -> playerRepository.findPlayerByPolemicaId(polemicaId)
            gomafiaId != null -> playerRepository.findPlayerByGomafiaId(gomafiaId)
            else -> playerRepository.findPlayerByNickname(nickname)
        }

        return existingPlayer ?: playerRepository.save(
            Player(nickname = nickname, polemicaId = polemicaId, gomafiaId = gomafiaId)
        )
    }

    /**
     * Возвращает Map с URL-ами для каждого типа фото для данного контекста.
     */
    fun getPhotoInfoForParticipant(
        player: Player,
        tournamentType: GameType?,
        tournamentId: Long?
    ): ParticipantView {
        val photos = player.playerPhotos.filter { !it.deleted }

        // Для каждого типа фото (MAIN, RED, SHER...) находим самый релевантный URL
        val allPhotoUrls = PhotoType.entries.associateWith { photoType ->
            // Приоритеты для поиска URL:
            // 1. Турнир + Роль
            photos.firstOrNull { it.tournamentType == tournamentType && it.tournamentId == tournamentId && it.type == photoType }?.url
            // 2. Роль (глобально)
                ?: photos.firstOrNull { it.tournamentId == null && it.type == photoType }?.url
        }

        return ParticipantView(player, allPhotoUrls)
    }

    // Получение набора фото для ролей в конкретном турнире (для цветных квадратиков)
    fun getAvailableRolePhotosForTournament(
        player: Player,
        tournamentType: GameType,
        tournamentId: Long
    ): Set<PhotoType> {
        return player.playerPhotos
            .filter { !it.deleted && it.tournamentType == tournamentType && it.tournamentId == tournamentId && it.type != PhotoType.MAIN }
            .map { it.type }
            .toSet()
    }

    @Transactional
    fun updatePlayerIds(playerId: UUID, polemicaId: Long?, gomafiaId: Long?): Player {
        val player = playerRepository.findById(playerId).orElseThrow { RuntimeException("Player not found") }
        polemicaId?.let { player.polemicaId = it }
        gomafiaId?.let { player.gomafiaId = it }
        return playerRepository.save(player)
    }

    @Transactional(readOnly = true)
    suspend fun getPlayerCardDetails(playerId: UUID): PlayerCardView {
        val player = playerRepository.findById(playerId).orElseThrow { RuntimeException("Player not found") }
        val allPhotos = player.playerPhotos.filter { !it.deleted }

        // 1. Глобальные фото (без привязки к турниру)
        val globalUrls = PhotoType.values().associateWith { type ->
            allPhotos.firstOrNull { it.tournamentId == null && it.type == type }?.url
        }

        // 2. Фото, сгруппированные по турнирам
        val tournamentPhotoGroups = allPhotos
            .filter { it.tournamentId != null }
            .groupBy { it.tournamentType!! to it.tournamentId!! }

        val tournamentViews = tournamentPhotoGroups.map { (tournamentKey, photos) ->
            val (type, id) = tournamentKey

            // Получаем название турнира
            val title = getTournamentTitle(type, id)

            val urls = PhotoType.entries.associateWith { photoType ->
                photos.firstOrNull { it.type == photoType }?.url
            }

            PlayerTournamentPhotoView(
                tournamentSource = type,
                tournamentId = id,
                tournamentTitle = title,
                photoUrls = urls
            )
        }

        return PlayerCardView(player, globalUrls, tournamentViews)
    }

    // Вспомогательная функция для получения названия турнира
    private suspend fun getTournamentTitle(source: GameType, id: Long): String {
        return when (source) {
            GameType.GOMAFIA -> gomafiaRestClient.getTournament(id.toInt())
                .tournamentDto.title ?: "Турнир GoMafia #${id}"

            GameType.POLEMICA -> polemicaClient.getCompetitions()
                .find { it.id == id }?.name ?: "Турнир Polemica #${id}"

            else -> "Кастомный турнир #${id}"
        }
    }

    // Поиск игроков по запросу
    fun searchPlayers(query: String): List<Player> {
        if (query.isBlank()) return emptyList()

        val polemicaId = query.toLongOrNull()
        val gomafiaId = query.toLongOrNull()

        return playerRepository.searchPlayers(query, polemicaId, gomafiaId)
    }

    // Проверка уникальности ID перед обновлением
    data class DuplicateCheckResult(
        val hasDuplicates: Boolean,
        val duplicatePolemicaPlayer: Player? = null,
        val duplicateGomafiaPlayer: Player? = null
    )

    fun checkForDuplicateIds(playerId: UUID, polemicaId: Long?, gomafiaId: Long?): DuplicateCheckResult {
        var duplicatePolemicaPlayer: Player? = null
        var duplicateGomafiaPlayer: Player? = null

        if (polemicaId != null) {
            duplicatePolemicaPlayer = playerRepository.findByPolemicaIdAndIdNot(polemicaId, playerId)
        }

        if (gomafiaId != null) {
            duplicateGomafiaPlayer = playerRepository.findByGomafiaIdAndIdNot(gomafiaId, playerId)
        }

        return DuplicateCheckResult(
            hasDuplicates = duplicatePolemicaPlayer != null || duplicateGomafiaPlayer != null,
            duplicatePolemicaPlayer = duplicatePolemicaPlayer,
            duplicateGomafiaPlayer = duplicateGomafiaPlayer
        )
    }

    // Объединение профилей игроков
    @Transactional
    fun mergePlayers(primaryPlayerId: UUID, secondaryPlayerId: UUID): Player {
        val primaryPlayer = playerRepository.findById(primaryPlayerId)
            .orElseThrow { RuntimeException("Primary player not found") }
        val secondaryPlayer = playerRepository.findById(secondaryPlayerId)
            .orElseThrow { RuntimeException("Secondary player not found") }

        // Переносим ID из вторичного профиля, если у основного их нет
        if (primaryPlayer.polemicaId == null && secondaryPlayer.polemicaId != null) {
            primaryPlayer.polemicaId = secondaryPlayer.polemicaId
        }
        if (primaryPlayer.gomafiaId == null && secondaryPlayer.gomafiaId != null) {
            primaryPlayer.gomafiaId = secondaryPlayer.gomafiaId
        }

        // Переносим все фотографии из вторичного профиля
        secondaryPlayer.playerPhotos.forEach { photo ->
            photo.id = null // Сбрасываем ID для создания новой записи
            primaryPlayer.playerPhotos.add(photo)
        }

        // Сохраняем изменения в основном профиле
        val updatedPrimaryPlayer = playerRepository.save(primaryPlayer)

        // Удаляем вторичный профиль
        playerRepository.delete(secondaryPlayer)

        return updatedPrimaryPlayer
    }

    /**
     * Returns the effective nickname for a player.
     * If customNickname is set, returns it; otherwise returns the external service nickname.
     */
    fun getEffectiveNickname(player: Player): String {
        return player.customNickname ?: player.nickname
    }

    /**
     * Updates the player's custom nickname and updates all existing GamePlayer records.
     * If customNickname is null or empty, clears the custom nickname (reverts to external service nickname).
     */
    @Transactional
    fun updatePlayerNickname(playerId: UUID, customNickname: String?): Player {
        val player = playerRepository.findById(playerId)
            .orElseThrow { RuntimeException("Player not found") }

        // Set customNickname (null or empty string clears it)
        val newCustomNickname = customNickname?.takeIf { it.isNotBlank() }
        player.customNickname = newCustomNickname

        val updatedPlayer = playerRepository.save(player)

        // Update all existing GamePlayer records for this player
        val effectiveNickname = getEffectiveNickname(updatedPlayer)

        // Update GamePlayers by polemicaId
        player.polemicaId?.let { polemicaId ->
            val gamePlayers = gamePlayerRepository.findBySourcePlayerId(polemicaId)
            gamePlayers.forEach { gamePlayer ->
                gamePlayer.nickname = effectiveNickname
            }
            gamePlayerRepository.saveAll(gamePlayers)
        }

        // Update GamePlayers by gomafiaId
        player.gomafiaId?.let { gomafiaId ->
            val gamePlayers = gamePlayerRepository.findBySourcePlayerId(gomafiaId)
            gamePlayers.forEach { gamePlayer ->
                gamePlayer.nickname = effectiveNickname
            }
            gamePlayerRepository.saveAll(gamePlayers)
        }

        return updatedPlayer
    }
}
