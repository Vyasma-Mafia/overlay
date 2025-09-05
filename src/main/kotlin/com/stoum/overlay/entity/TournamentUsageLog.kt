package com.stoum.overlay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "tournament_usage_log")
data class TournamentUsageLog(
    @Id
    @Column(name = "tournament_id", nullable = false)
    val tournamentId: Long,

    @Column(name = "game_count", nullable = false)
    var gameCount: Long = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tables", columnDefinition = "jsonb")
    var tables: MutableSet<Int> = mutableSetOf(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
