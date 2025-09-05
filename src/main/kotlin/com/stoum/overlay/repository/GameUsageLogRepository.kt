package com.stoum.overlay.repository

import com.stoum.overlay.entity.GameUsageLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GameUsageLogRepository : JpaRepository<GameUsageLog, Long> {
    fun existsByGameIdAndTournamentId(gameId: UUID, tournamentId: Long): Boolean
}