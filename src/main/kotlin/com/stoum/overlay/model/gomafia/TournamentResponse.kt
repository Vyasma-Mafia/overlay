package com.stoum.overlay.model.gomafia

data class TournamentResponse(
        val tournamentDto: TournamentDto,
        val games: List<GameDto>,
        val tournamentResults: List<TournamentResultDto>?
)