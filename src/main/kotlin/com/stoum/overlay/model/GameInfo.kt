package com.stoum.overlay.model

class GameInfo {
    class PlayerInfo(
        val nickname: String,
        val photoUrl: String,
        var role: String? = null,
        var status: Pair<String, String>? = null,
        var checks: MutableList<Pair<String, String>>? = mutableListOf(),
        var stat: MutableMap<String, Pair<String, String>>? = mutableMapOf()
    )

    var players: MutableList<PlayerInfo> = mutableListOf()
}