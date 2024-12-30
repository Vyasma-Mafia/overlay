package com.stoum.overlay.repository

import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface GameRepository : CrudRepository<Game, UUID> {
    fun findGameByTournamentIdAndGameNumAndTableNum(tournamentId: Int, gameNum: Int, tableNum: Int): Game?
    fun findGameByTypeAndStarted(type: GameType, startred: Boolean): List<Game>
}
