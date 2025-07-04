package com.stoum.overlay.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.stoum.overlay.entity.converters.MapListConverter
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import jakarta.persistence.CascadeType
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(
    name = "game",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tournamentId", "gameNum", "tableNum", "phase", "type"])]
)
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
        @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
        @JoinColumn(name = "game_id")
        @OrderBy("place ASC")
    var players: MutableList<GamePlayer> = mutableListOf(),
    var started: Boolean? = null,
    var visibleOverlay: Boolean? = true,
    var visibleRoles: Boolean? = true,
    var visibleScores: Boolean? = true,
    var text: String? = null,
    var result: String? = null,
    var delay: Int = 0,
    var autoNextGame: Boolean? = true,
    @Convert(converter = MapListConverter::class)
    var voteCandidates: MutableList<Map<String, String>>? = arrayListOf(),

    @Version
    private var version: Long? = 0
) {

    @Transient
    @JsonInclude
    var playersOrdered = listOf<String>()
}
