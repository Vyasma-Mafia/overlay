package com.stoum.overlay.entity.gomafia

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.StringJoiner
import java.util.UUID

@Entity
@Table(name = "gm_game_player")
class GomafiaGamePlayer (
        val userId: Int,
        val gameId: Long,
        val login: String,
        val place: Int,
        val role: String?,
        val points: BigDecimal,
        val type: String?,
        val eloDelta: Int,
        @Id
        @GeneratedValue
        var id: Long? = null,
)