package com.stoum.overlay.entity.gomafia

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "gm_game")
class GomafiaGame(
        val tournamentId: Int,
        val gameNumber: Int,
        val tableNumber: Int,
        val refereeLogin: String?,
        val win: String,
) {
        @Id
        @GeneratedValue
        var id: Long? = null
}