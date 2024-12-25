package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaUser
import java.time.LocalDateTime

interface PolemicaClient {

    fun getGameFromClub(clubGameId: PolemicaClubGameId): PolemicaGame
    fun getGamesFromClub(clubId: Long, offset: Long, limit: Long): List<PolemicaGameReference>
    fun getCompetitions(): List<PolemicaCompetition>
    fun getGamesFromCompetition(id: Long): List<PolemicaGameReference>
    fun getGameFromCompetition(polemicaCompetitionGameId: PolemicaClient.PolemicaCompetitionGameId): PolemicaGame

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PolemicaCompetition(
        val id: Long,
        val name: String,
        val startDate: LocalDateTime?,
        val endDate: LocalDateTime?,
        val region: String?,
        val city: String?,
        val description: String?,
        val link: String?,
        val scoringType: Int?,
        val scoringVersion: String?,
        val showRating: Int?,
        val memberCount: Int?,
        val rating: Int?,
        val phoneRequired: Boolean?,
        val winScores: Int?,
        val hasScores: Boolean?
    )

    data class PolemicaClubGameId(val clubId: Long, val gameId: Long, val version: Long? = null)
    data class PolemicaCompetitionGameId(val competitionId: Long, val gameId: Long, val version: Long? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PolemicaGameReference(
        val id: Long,
        val started: LocalDateTime,
        val result: PolemicaGameResult?,
        val referee: PolemicaUser,
        val version: Long?
    )
}
