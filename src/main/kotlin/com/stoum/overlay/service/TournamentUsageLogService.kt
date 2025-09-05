package com.stoum.overlay.service

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.stoum.overlay.entity.GameUsageLog
import com.stoum.overlay.entity.TournamentUsageLog
import com.stoum.overlay.model.TournamentUsageStat
import com.stoum.overlay.repository.GameUsageLogRepository
import com.stoum.overlay.repository.TournamentUsageLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TournamentUsageLogService(
    private val gameUsageLogRepository: GameUsageLogRepository,
    private val tournamentUsageLogRepository: TournamentUsageLogRepository,
    private val polemicaClient: PolemicaClient
) {

    @Transactional
    fun logGameUsage(gameId: UUID, tournamentId: Long, tableNum: Int) {
        if (!gameUsageLogRepository.existsByGameIdAndTournamentId(gameId, tournamentId)) {
            // 1. Log the specific game to prevent duplicates
            gameUsageLogRepository.save(
                GameUsageLog(
                    gameId = gameId,
                    tournamentId = tournamentId,
                    tableNum = tableNum
                )
            )

            // 2. Find or create the aggregate log for the tournament
            val tournamentLog = tournamentUsageLogRepository.findById(tournamentId)
                .orElseGet { TournamentUsageLog(tournamentId = tournamentId) }

            // 3. Update the aggregate counters
            tournamentLog.gameCount++
            tournamentLog.tables.add(tableNum)

            tournamentUsageLogRepository.save(tournamentLog)
        }
    }

    @Transactional(readOnly = true)
    fun getTournamentUsageStats(): List<TournamentUsageStat> {
        val allLogs = tournamentUsageLogRepository.findAll().sortedByDescending { it.updatedAt }
        val competitions = polemicaClient.getCompetitions().associateBy { it.id }

        return allLogs.map { log ->
            val competition = competitions[log.tournamentId]
            TournamentUsageStat(
                tournamentId = log.tournamentId,
                tournamentName = competition?.name,
                tournamentLocation = competition?.city,
                gameCount = log.gameCount,
                tableCount = log.tables.size.toLong()
            )
        }
    }
}
