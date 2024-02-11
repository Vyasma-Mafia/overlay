package com.stoum.overlay.entity

import com.stoum.overlay.entity.enums.PhotoType
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "player_photo")
class PlayerPhoto (
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        var id: UUID? = null,
        var url: String,
        var description: String,
        @Enumerated(EnumType.STRING)
        var type: PhotoType,
)