package com.stoum.overlay.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonManagedReference
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "game")
data class Game(
        @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
        @Enumerated(EnumType.STRING)
        var type: GameType,
        var tournamentId: Int? = null,
        var gameNum: Int? = null,
        var tableNum: Int? = null,
    var phase: Int? = null,
    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], mappedBy = "game", orphanRemoval = true)
    @JsonManagedReference("game-players")
        @OrderBy("place ASC")
    var players: MutableList<GamePlayer> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var facts: MutableList<Fact> = mutableListOf(),
    var started: Boolean? = null,
    var manuallyStarted: Boolean? = true,
    var visibleOverlay: Boolean? = true,
    var visibleRoles: Boolean? = true,
    var visibleScores: Boolean? = true,
    var text: String? = null,
    var result: String? = null,
    var delay: Int = 0,
    var autoNextGame: Boolean? = true,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var voteCandidates: MutableList<Map<String, String>>? = arrayListOf(),

    // Поля для отслеживания ошибок краулинга
    var crawlFailureCount: Int? = null,
    var lastCrawlError: String? = null,
    var lastFailureTime: LocalDateTime? = null,
    var crawlStopReason: String? = null,

    @Version
    private var version: Long? = 0
) {

    @Transient
    @JsonInclude
    var playersOrdered = listOf<String>()
}
