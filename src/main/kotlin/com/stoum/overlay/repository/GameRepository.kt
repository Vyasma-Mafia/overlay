package com.stoum.overlay.repository

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GameRepository : JpaRepository<Game, UUID> {
    fun findGameByTournamentIdAndGameNumAndTableNumAndPhase(
        tournamentId: Int,
        gameNum: Int,
        tableNum: Int,
        phase: Int?
    ): Game?

    fun findGamesByTournamentIdAndGameNumAndTableNum(tournamentId: Int, gameNum: Int, tableNum: Int): List<Game>
    fun findGameByTypeAndStarted(type: GameType, started: Boolean): List<Game>
    fun findGamesByTournamentId(tournamentId: Int): List<Game>
}
