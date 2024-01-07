package com.stoum.overlay.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "player")
class Player (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    var nickname: String,
    var photoUrl: String? = null,
    var role: String? = null,
    var status: String? = null,
    var checks: String? = null,
    var stat: String? = null,
    @JoinColumn(name = "game", referencedColumnName = "id")
    var gameId: UUID
)