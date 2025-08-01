package com.stoum.overlay.repository

import com.stoum.overlay.entity.TournamentOverlaySettings
import com.stoum.overlay.entity.enums.GameType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TournamentOverlaySettingsRepository : JpaRepository<TournamentOverlaySettings, UUID> {
    fun findByGameTypeAndTournamentId(
        gameType: GameType,
        tournamentId: Long
    ): TournamentOverlaySettings?
}
