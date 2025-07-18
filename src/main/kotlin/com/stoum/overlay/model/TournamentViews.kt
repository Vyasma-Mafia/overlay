package com.stoum.overlay.model

import com.stoum.overlay.entity.Player
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType

// DTO для отображения в списке турниров
data class UnifiedTournamentView(
    val id: String,
    val source: GameType,
    val title: String,
    val dates: String,
    val location: String,
    val participantsCount: Int?
)

// DTO для отображения участника в списке
data class ParticipantView(
    val player: Player,
    // Ключевое изменение: передаем Map<PhotoType, String?>, где String - это URL фото, если оно есть
    val photoUrls: Map<PhotoType, String?>
)

// DTO для всей страницы карточки игрока
data class PlayerCardView(
    val player: Player,
    val globalPhotoUrls: Map<PhotoType, String?>,
    val tournamentPhotos: List<PlayerTournamentPhotoView>
)

// DTO для каждой карточки турнира на странице игрока
data class PlayerTournamentPhotoView(
    val tournamentSource: GameType,
    val tournamentId: Long,
    val tournamentTitle: String, // Нам понадобится способ получить это название
    val photoUrls: Map<PhotoType, String?>
)

