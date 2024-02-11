package com.stoum.overlay.model

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.overlay.GamePlayer

class GameInfo() {
    class PlayerInfo(
        val nickname: String,
        val photoUrl: String,
        var role: String? = null,
        var status: Pair<String, String>? = null,
        var checks: MutableList<Map<String, String>>? = mutableListOf(),
        var stat: Map<String, Map<String, String>>? = mutableMapOf()
    ) {
        constructor(player: GamePlayer) : this(
            player.nickname,
            player.photoUrl!!,
            player.role,
            player.status,
            player.checks,
            player.stat
        )
    }

    var players: MutableList<PlayerInfo> = mutableListOf()

    constructor(game: Game) : this() {
        game.players.forEach { p ->
            players.add(PlayerInfo(p))
        }
        game.players.sortBy { p -> p.place }
    }
}