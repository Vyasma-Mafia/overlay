package com.stoum.overlay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "game_usage_log",
    uniqueConstraints = [UniqueConstraint(columnNames = ["game_id", "tournament_id"])]
)
data class GameUsageLog(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,

    @Column(name = "game_id", nullable = false)
    val gameId: UUID,

    @Column(name = "tournament_id", nullable = false)
    val tournamentId: Long,

    @Column(name = "table_num", nullable = false)
    val tableNum: Int,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)
