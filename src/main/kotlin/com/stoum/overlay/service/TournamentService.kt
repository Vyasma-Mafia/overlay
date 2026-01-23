package com.stoum.overlay.service

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.getLogger
import com.stoum.overlay.model.ParticipantView
import com.stoum.overlay.model.UnifiedTournamentView
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import com.stoum.overlay.service.mafiauniverse.MafiaUniverseClient
import com.stoum.overlay.service.mafiauniverse.MafiaUniverseHtmlParser
import org.springframework.stereotype.Service

@Service
class TournamentService(
    private val gomafiaClient: GomafiaRestClient,
    private val polemicaClient: PolemicaClient,
    private val mafiaUniverseClient: MafiaUniverseClient?,
    private val mafiaUniverseHtmlParser: MafiaUniverseHtmlParser?,
    private val playerService: PlayerService
) {
    val log = this.getLogger()

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

            GameType.MAFIAUNIVERSE -> {
                if (mafiaUniverseClient == null || mafiaUniverseHtmlParser == null) {
                    emptyList()
                } else {
                    val tournamentsHtml = mafiaUniverseClient.getTournamentsPage()
                    val tournaments = mafiaUniverseHtmlParser.parseTournamentsList(tournamentsHtml)
                    tournaments.map {
                        UnifiedTournamentView(
                            id = it.id.toString(),
                            source = GameType.MAFIAUNIVERSE,
                            title = it.name,
                            dates = "", // MafiaUniverse HTML doesn't provide dates in tournaments list
                            location = "", // MafiaUniverse HTML doesn't provide location in tournaments list
                            participantsCount = null // Will be calculated when needed
                        )
                    }
                }
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

            GameType.MAFIAUNIVERSE -> {
                if (mafiaUniverseClient == null || mafiaUniverseHtmlParser == null) {
                    emptyList()
                } else {
                    // Get tournament players page
                    val playersPageHtml = mafiaUniverseClient.getTournamentPlayersPage(tournamentId.toInt())
                    val parsedPlayers = mafiaUniverseHtmlParser.parseTournamentPlayers(playersPageHtml)

                    // Create or find players by nickname
                    parsedPlayers.map { parsedPlayer ->
                        val player = playerService.findOrCreatePlayerByMafiaUniverseNickname(parsedPlayer.nickname)
                        playerService.getPhotoInfoForParticipant(player, source, tournamentId.toLong())
                    }
                }
            }

            else -> emptyList()
        }
    }
}
