package com.stoum.overlay.entity

import com.stoum.overlay.entity.overlay.GamePlayer
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "player")
class Player (
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        var id: UUID? = null,
        var nickname: String,
        @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
        @JoinColumn(name = "player_id")
        var playerPhotos: MutableList<PlayerPhoto> = mutableListOf(),
)