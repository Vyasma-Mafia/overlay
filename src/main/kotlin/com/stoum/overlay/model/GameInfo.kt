package com.stoum.overlay.model

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.Player

class GameInfo() {
    class PlayerInfo(
        val nickname: String,
        val photoUrl: String,
        var role: String? = null,
        var status: Pair<String, String>? = null,
        var checks: MutableList<Pair<String, String>>? = mutableListOf(),
        var stat: MutableMap<String, Pair<String, String>>? = mutableMapOf()
    ) {
        constructor(player: Player) : this(
            player.nickname,
            player.photoUrl!!,
            player.role
        )
    }

    var players: MutableList<PlayerInfo> = mutableListOf()

    constructor(game: Game) : this() {
        game.players?.forEach { p ->
            players.add(PlayerInfo(p))
        }
    }
}