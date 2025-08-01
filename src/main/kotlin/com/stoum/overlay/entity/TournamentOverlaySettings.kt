package com.stoum.overlay.entity

import com.stoum.overlay.entity.enums.GameType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "tournament_overlay_settings",
    uniqueConstraints = [UniqueConstraint(columnNames = ["game_type", "tournament_id"])]
)
class TournamentOverlaySettings(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    val gameType: GameType,

    @Column(name = "tournament_id", nullable = false)
    val tournamentId: Long,

    @Column(name = "overlay_enabled", nullable = false)
    var overlayEnabled: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
