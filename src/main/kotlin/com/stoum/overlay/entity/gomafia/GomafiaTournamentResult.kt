package com.stoum.overlay.entity.gomafia

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "gm_tournament_result")
class GomafiaTournamentResult (
        val tournamentId: Int,
        val login: String,
        val place: Int,
        val sum: BigDecimal,
        val sumExtra: BigDecimal,
        val win: Int,
        val winDon: Int,
        val winSher: Int,
        val compensation: BigDecimal,
        val extraPoints: BigDecimal,
        val fine: BigDecimal,
        val firstKill: Int,
        val firstKillPoints: BigDecimal,
        val gg: BigDecimal,
        @Id
        @GeneratedValue
        val id: Long? = null
)