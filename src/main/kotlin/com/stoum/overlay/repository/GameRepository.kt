package com.stoum.overlay.repository

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GameRepository : JpaRepository<Game, UUID> {
    fun findGameByTournamentIdAndGameNumAndTableNumAndPhaseAndType(
        tournamentId: Int,
        gameNum: Int,
        tableNum: Int,
        phase: Int?,
        type: GameType
    ): Game?

    fun findGamesByTournamentIdAndGameNumAndTableNum(tournamentId: Int, gameNum: Int, tableNum: Int): List<Game>
    fun findGameByTypeAndStarted(type: GameType, started: Boolean): List<Game>
    fun findGamesByTournamentId(tournamentId: Int): List<Game>

    fun findFirstByTournamentIdAndResultIsNullOrderByPhaseAscGameNumAsc(tournamentId: Int): Game?

    fun findFirstByTournamentIdAndTableNumAndResultIsNullOrderByPhaseAscGameNumAsc(
        tournamentId: Int,
        tableNum: Int
    ): Game?

    // Методы для работы с проблемными играми
    fun findGamesByTypeAndStartedAndCrawlFailureCountGreaterThan(
        type: GameType,
        started: Boolean,
        failureCount: Int
    ): List<Game>

    fun findGamesByTypeAndStartedAndCrawlStopReasonIsNotNull(
        type: GameType,
        started: Boolean
    ): List<Game>
}
