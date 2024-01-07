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
    @OneToMany(targetEntity = Player::class, fetch = FetchType.EAGER)
    var players: List<Player>? = null,
)