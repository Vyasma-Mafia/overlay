package com.stoum.overlay.entity

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.persistence.*
import java.util.UUID
import kotlin.jvm.Transient

@Entity
@Table(name = "game")
class Game (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    var type: GameType,
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    var players: MutableList<Player> = mutableListOf(),
) {

    @Transient
    @JsonInclude
    var playersOrdered = listOf<String>()
}