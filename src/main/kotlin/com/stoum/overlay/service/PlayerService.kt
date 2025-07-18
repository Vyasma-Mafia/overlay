package com.stoum.overlay.service

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType
import com.stoum.overlay.model.ParticipantView
import com.stoum.overlay.model.PlayerCardView
import com.stoum.overlay.model.PlayerTournamentPhotoView
import com.stoum.overlay.repository.PlayerRepository
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PlayerService(
    val playerRepository: PlayerRepository,
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
}
