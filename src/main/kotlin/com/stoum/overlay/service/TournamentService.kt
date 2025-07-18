package com.stoum.overlay.service

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.model.ParticipantView
import com.stoum.overlay.model.UnifiedTournamentView
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import org.springframework.stereotype.Service

@Service
class TournamentService(
    private val gomafiaClient: GomafiaRestClient,
    private val polemicaClient: PolemicaClient,
    private val playerService: PlayerService
) {

    // Получение списка турниров из указанного источника
    suspend fun getTournaments(source: GameType): List<UnifiedTournamentView> {
        return when (source) {
            GameType.GOMAFIA -> gomafiaClient.getTournaments().map {
                UnifiedTournamentView(
                    id = it.id!!,
                    source = GameType.GOMAFIA,
                    title = it.title ?: "Без названия",
                    dates = "${it.dateStart} - ${it.dateEnd}",
                    location = "${it.cityTranslate}, ${it.countryTranslate}",
                    participantsCount = it.projectedCountOfParticipant?.toIntOrNull()
                )
            }

            GameType.POLEMICA -> polemicaClient.getCompetitions().map {
                UnifiedTournamentView(
                    id = it.id.toString(),
                    source = GameType.POLEMICA,
                    title = it.name,
                    dates = "${it.startDate} - ${it.endDate}",
                    location = "${it.city}, ${it.region}",
                    participantsCount = it.memberCount
                )
            }

            else -> emptyList()
        }
    }

    // Получение участников турнира
    suspend fun getTournamentParticipants(source: GameType, tournamentId: String): List<ParticipantView> {
        return when (source) {
            GameType.GOMAFIA -> {
                val tournamentDetails = gomafiaClient.getTournament(tournamentId.toInt())
                // Извлекаем уникальных игроков
                val uniquePlayers = tournamentDetails.games
                    .flatMap { it.table }
                    .distinctBy { it.id }

                uniquePlayers.map { playerDto ->
                    val player = playerService.findOrCreatePlayer(
                        nickname = playerDto.login!!,
                        gomafiaId = playerDto.id?.toLong()
                    )
                    playerService.getPhotoInfoForParticipant(player, source, tournamentId.toLong())
                }
            }

            GameType.POLEMICA -> {
                val members = polemicaClient.getCompetitionMembers(tournamentId.toLong())
                members.map { member ->
                    val player = playerService.findOrCreatePlayer(
                        nickname = member.player.username,
                        polemicaId = member.player.id
                    )
                    playerService.getPhotoInfoForParticipant(player, source, tournamentId.toLong())
                }
            }

            else -> emptyList()
        }
    }
}
