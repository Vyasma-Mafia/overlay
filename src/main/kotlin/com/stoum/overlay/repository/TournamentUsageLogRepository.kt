package com.stoum.overlay.repository

import com.stoum.overlay.entity.TournamentUsageLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TournamentUsageLogRepository : JpaRepository<TournamentUsageLog, Long> {
}