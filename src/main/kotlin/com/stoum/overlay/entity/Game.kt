package com.stoum.overlay.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.entity.overlay.GamePlayer
import jakarta.persistence.*
import java.util.UUID
import kotlin.jvm.Transient

@Entity
@Table(name = "game")
class Game (
        @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
        @Enumerated(EnumType.STRING)
        var type: GameType,
        var tournamentId: Int? = null,
        var gameNum: Int? = null,
        var tableNum: Int? = null,
        @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "game_id")
    var players: MutableList<GamePlayer> = mutableListOf(),
) {

    @Transient
    @JsonInclude
    var playersOrdered = listOf<String>()
}