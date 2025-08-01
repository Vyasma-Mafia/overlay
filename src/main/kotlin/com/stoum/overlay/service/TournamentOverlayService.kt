package com.stoum.overlay.service

import com.stoum.overlay.entity.TournamentOverlaySettings
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.exception.InvalidPasswordException
import com.stoum.overlay.exception.TournamentSettingsNotFoundException
import com.stoum.overlay.repository.TournamentOverlaySettingsRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentOverlayService(
    private val repository: TournamentOverlaySettingsRepository,
    @Value("\${app.overlay.admin.password}")
    private val adminPassword: String
) {

    @Transactional(readOnly = true)
    fun getSettings(gameType: GameType, tournamentId: Long): TournamentOverlaySettings {
        return repository.findByGameTypeAndTournamentId(gameType, tournamentId)
            ?: TournamentOverlaySettings(
                gameType = gameType,
                tournamentId = tournamentId
            )
    }

    @Transactional
    fun toggleOverlay(
        gameType: GameType,
        tournamentId: Long,
        password: String,
        enabled: Boolean
    ): TournamentOverlaySettings {
        if (password != adminPassword) {
            throw InvalidPasswordException("Incorrect password for overlay settings")
        }

        val settings = repository.findByGameTypeAndTournamentId(gameType, tournamentId)
            ?: TournamentOverlaySettings(
                gameType = gameType,
                tournamentId = tournamentId
            )

        settings.overlayEnabled = enabled
        return repository.save(settings)
    }
}
