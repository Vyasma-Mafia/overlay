package com.stoum.overlay.entity

import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.enums.PhotoType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "player_photo")
class PlayerPhoto (
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        var id: UUID? = null,
        var url: String,
        @Enumerated(EnumType.STRING)
        var type: PhotoType = PhotoType.MAIN,
        var tournamentType: GameType? = null,
        var tournamentId: Long? = null,
        var deleted: Boolean = false
)
