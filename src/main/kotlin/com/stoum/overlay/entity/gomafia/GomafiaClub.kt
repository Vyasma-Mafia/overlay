package com.stoum.overlay.entity.gomafia

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "gm_club")
class GomafiaClub (
        @Id
        val id: Int,
        val avatarLink: String?,
        val clubScore: Int,
        val eloAverage: BigDecimal,
        val title: String,
        val city: String,
        val country: String,
)