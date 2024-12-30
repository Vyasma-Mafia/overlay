package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import org.springframework.web.reactive.function.client.WebClient

class PolemicaClientImpl(private val polemicaWebClient: WebClient) : PolemicaClient {

    override fun getGameFromClub(clubGameId: PolemicaClient.PolemicaClubGameId): PolemicaGame {
        return polemicaWebClient.get()
            .uri(
                "/v1/clubs/${clubGameId.clubId}/games/${clubGameId.gameId}" + getVersionQueryParam(clubGameId.version)
            )
            .retrieve()
            .bodyToMono(PolemicaGame::class.java)
            .block() ?: throw RuntimeException("Get game from club error")
    }

    private fun getVersionQueryParam(version: Long?) =
        if (version != null) {
            "?version=$version"
        } else {
            ""
        }

    override fun getGamesFromClub(
        clubId: Long,
        offset: Long,
        limit: Long
    ): List<PolemicaClient.PolemicaClubGameReference> {
        return polemicaWebClient.get()
            .uri("/v1/clubs/${clubId}/games?offset=${offset}&limit=${limit}")
            .retrieve()
            .bodyToFlux(PolemicaClient.PolemicaClubGameReference::class.java)
            .collectList()
            .block() ?: throw RuntimeException("Get games from club error")
    }

    override fun getCompetitions(): List<PolemicaClient.PolemicaCompetition> {
        return polemicaWebClient.get()
            .uri("/v1/competitions")
            .retrieve()
            .bodyToFlux(PolemicaClient.PolemicaCompetition::class.java)
            .collectList()
            .block() ?: throw RuntimeException("Get competitions error")
    }

    override fun getGamesFromCompetition(id: Long): List<PolemicaClient.PolemicaTournamentGameReference> {
        return polemicaWebClient.get()
            .uri("/v1/competitions/${id}/games")
            .retrieve()
            .bodyToFlux(PolemicaClient.PolemicaTournamentGameReference::class.java)
            .collectList()
            .block() ?: throw RuntimeException("Get games from competition error")
    }

    override fun getGameFromCompetition(polemicaCompetitionGameId: PolemicaClient.PolemicaCompetitionGameId): PolemicaGame {
        return polemicaWebClient.get()
            .uri(
                "/v1/competitions/${polemicaCompetitionGameId.competitionId}/games/${polemicaCompetitionGameId.gameId}"
                    + getVersionQueryParam(polemicaCompetitionGameId.version)
            )
            .retrieve()
            .bodyToMono(PolemicaGame::class.java)
            .block() ?: throw RuntimeException("Get game from competition error")
    }
}
