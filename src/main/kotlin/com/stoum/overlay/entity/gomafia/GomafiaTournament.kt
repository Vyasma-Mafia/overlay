package com.stoum.overlay.entity.gomafia

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.Date

@Entity
@Table(name = "gm_tournament")
class GomafiaTournament (
        @Id
        val id: Int,
        val title: String,
        val date: Date,
        val type: String,
        val city: String,
        val country: String,
        val status: String,
        val orgId: Int,
        val isFsm: String,
        val star: Int,
        val eloAverage: BigDecimal,
)