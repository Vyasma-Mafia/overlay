package com.stoum.overlay.entity.gomafia

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "gm_user")
class GomafiaUser(
        @Id
        val id: Int,
        val elo: BigDecimal,
        val login: String,
        val avatarLink: String?,
        val country: String?,
        val city: String?,
        val tournamentsPlayed: Int,
        val tournamentsScore: Int,
        var clubId: Int?,
        val region: String?
)