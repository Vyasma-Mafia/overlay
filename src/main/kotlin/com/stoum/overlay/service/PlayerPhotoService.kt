package com.stoum.overlay.service

import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.PlayerPhoto
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType
import com.stoum.overlay.repository.PlayerRepository
import org.springframework.stereotype.Service

const val DEFAULT_PHOTO_URL = "https://storage.yandexcloud.net/mafia-photos/null.jpg"

@Service
class PlayerPhotoService(val playerRepository: PlayerRepository) {

    fun getPlayerPhotoForCompetitionRole(
        player: Player,
        tournamentType: GameType,
        tournamentId: Long,
        role: String?
    ): PlayerPhoto? {
        val existingPlayerPhotos = player.playerPhotos.filter { !it.deleted }
        val firstPhoto = existingPlayerPhotos.find {
            it.tournamentType == tournamentType
                && tournamentId == it.tournamentId
                && it.type == roleToPhotoType(role)
        }
        if (firstPhoto != null) {
            return firstPhoto
        }
        val secondPhoto = existingPlayerPhotos.find {
            it.tournamentType == tournamentType
                && tournamentId == it.tournamentId
                && it.type == PhotoType.MAIN
        }
        if (secondPhoto != null) {
            return secondPhoto
        }
        val thirdPhoto = existingPlayerPhotos.find {
            it.tournamentType == null && it.tournamentId == null
                && it.type == roleToPhotoType(role)
        }
        if (thirdPhoto != null) {
            return thirdPhoto
        }
        val fourthPhoto = existingPlayerPhotos.find {
            it.tournamentType == null && it.tournamentId == null
                && it.type == PhotoType.MAIN
        }
        if (fourthPhoto != null) {
            return fourthPhoto
        }
        return null
    }

    fun getPlayerPhotoUrlForPlayerCompetitionRole(
        playerId: Long,
        tournamentType: GameType,
        tournamentId: Long,
        role: String?
    ): String {
        val player = when (tournamentType) {
            GameType.POLEMICA -> playerRepository.findPlayerByPolemicaId(playerId)
            GameType.GOMAFIA -> playerRepository.findPlayerByGomafiaId(playerId)
            GameType.CUSTOM -> null
        }
        if (player != null) {
            val playerPhoto = getPlayerPhotoForCompetitionRole(player, tournamentType, tournamentId, role)
            if (playerPhoto != null) {
                return playerPhoto.url
            }
        }
        return when (tournamentType) {
            GameType.POLEMICA -> "https://storage.yandexcloud.net/mafia-photos/${playerId}.jpg"
            GameType.GOMAFIA -> "https://storage.yandexcloud.net/mafia-photos/gomafia/${playerId}.jpg"
            GameType.CUSTOM -> DEFAULT_PHOTO_URL
        }
    }

    private fun roleToPhotoType(role: String?): PhotoType = when (role) {
        "don" -> PhotoType.DON
        "black" -> PhotoType.BLACK
        "red" -> PhotoType.RED
        "sher" -> PhotoType.SHER
        else -> PhotoType.MAIN
    }
}
