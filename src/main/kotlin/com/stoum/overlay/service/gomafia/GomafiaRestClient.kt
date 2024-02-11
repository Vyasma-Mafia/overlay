package com.stoum.overlay.service.gomafia

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.stoum.overlay.model.gomafia.*
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GomafiaRestClient {
    val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun getClubResidents(clubId: Int): List<UserDto> {
        val resp = RestClient.builder().baseUrl("https://gomafia.pro/api").build()
                .post()
                .uri("/club/getResidents")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("id=$clubId&limit=1000&offset=0")
                .retrieve()
                .body(String::class.java)

        return getDataList(resp!!).map { user -> mapper.convertValue(user, UserDto::class.java)}
    }

    fun getUsers(year: Int, region: GomafiaRegion): List<UserDto> {
        val resp = RestClient.builder().baseUrl("https://gomafia.pro/api").build()
                .post()
                .uri("/user/getTop")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("year=$year&region=${region.region}&limit=10000&offset=0")
                .retrieve()
                .body(String::class.java)

        return getDataList(resp!!).map { user -> mapper.convertValue(user, UserDto::class.java)}
    }

    fun getUserWithStats(id: Int): UserWithStats {
        val resp = RestClient.builder().baseUrl("https://gomafia.pro/api").build()
                .post()
                .uri("/stats/get")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("id=$id&period=all&gameType=all&tournamentType=all")
                .retrieve()
                .body(String::class.java)

        val data = getDataMap(resp!!)

        return mapUserWithStats(data)
    }

    fun getClubs(): List<ClubDto> {
        val resp = RestClient.builder().baseUrl("https://gomafia.pro/api").build()
                .post()
                .uri("/club/getTop")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("limit=10000&offset=0")
                .retrieve()
                .body(String::class.java)

        return getDataList(resp!!).map { club -> mapper.convertValue(club, ClubDto::class.java)}
    }

    fun getTournaments(): List<TournamentDto> {
        val resp = RestClient.builder().baseUrl("https://gomafia.pro/api").build()
                .post()
                .uri("/tournament/getAll")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("time=finished&fsm=yes&type=all&limit=10000&offset=0")
                .retrieve()
                .body(String::class.java)

        val data = getDataList(resp!!)

        return data.map { mapper.convertValue(it, TournamentDto::class.java) }
    }

    fun getTournament(id: Int): TournamentResponse {
        val resp = RestClient.builder().baseUrl("https://gomafia.pro/api").build()
                .post()
                .uri("/tournament/get")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("id=$id")
                .retrieve()
                .body(String::class.java)

        val data = getDataMap(resp!!)

        val games = (data["games"] as ArrayList<*>).flatMap { g  ->
            val gMap = (g as Map<*, *>)
            val gNum = gMap["gameNum"] as Int
            (gMap["game"] as ArrayList<*>).map { gdto ->
                val gg = mapper.convertValue(gdto, GameDto::class.java)
                gg.gameNum = gNum
                return@map gg
            }
        }

        //val results = (data["tournament_result"] as ArrayList<*>).map { r -> mapper.convertValue(r, TournamentResultDto::class.java) }

        val tournament = mapper.convertValue(data["tournament"]!!, TournamentDto::class.java)

        return TournamentResponse(tournament, games, null)
    }

    private fun getDataMap(response: String): Map<*, *> {
        val mapped = mapper.readValue(response, Map::class.java)
        return mapped["data"] as Map<*, *>
    }

    private fun getDataList(response: String): List<*> {
        val mapped = mapper.readValue(response, Map::class.java)
        return mapped["data"] as List<*>
    }

    private fun mapUserWithStats(data: Map<*, *>): UserWithStats {
        val user = mapper.convertValue(data["user"], UserDto::class.java)
        val statsMap = data["stats"]
        val primary = mapper.convertValue(data["stats"].gm("primary")!!, StatsDto.Primary::class.java )
        primary.totalGames = statsMap.gm("primary").gm("total_games") as Int
        val winrate = mapper.convertValue((data["stats"] as Map<String, Any>)["win_rate"]!!, StatsDto.WinRate::class.java )
        winrate.totalGames = statsMap.gm("win_rate").gm("total_games") as Int
        winrate.totalWins = StatsDto.TotalWins(
                statsMap.gm("win_rate").gm("total_wins").gm("value") as Int,
                statsMap.gm("win_rate").gm("total_wins").gm("percent") as Int,
        )
        val gamesStats = StatsDto.GamesStats(
                statsMap.gm("games_stats").gm("average_points") as Double,
                statsMap.gm("games_stats").gm("prize_places") as Int,
        )
        val advancedPoints = mapper.convertValue((data["stats"] as Map<String, Any>)["advanced_points"]!!, StatsDto.AdvancedPoints::class.java )

        val points10Games = statsMap.gm("advanced_points").gm("points_10_games")
        advancedPoints.points10Games =
                when(points10Games) {
                    is Int -> points10Games.toDouble()
                    is Double -> points10Games
                    else -> 0.0
                }

        return UserWithStats(user, StatsDto(primary, winrate, gamesStats, advancedPoints))
    }

    fun Any?.gm(key: String): Any? {
        return (this as Map<*, *>)[key]
    }
}