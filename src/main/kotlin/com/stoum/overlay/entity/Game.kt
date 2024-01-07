package com.stoum.overlay.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "game")
class Game (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    var type: GameType,
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    var players: MutableList<Player> = mutableListOf(),
)