package com.stoum.overlay.model

data class TournamentUsageStat(
    val tournamentId: Long,
    val tournamentName: String?,
    val tournamentLocation: String?,
    val gameCount: Long,
    val tableCount: Long
)