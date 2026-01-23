package com.stoum.overlay.service.mafiauniverse

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
@ConditionalOnProperty(value = ["app.mafiauniverse.enable"], havingValue = "true", matchIfMissing = false)
class MafiaUniverseClient(
    @Value("\${app.mafiauniverse.baseUrl:https://mafiauniverse.org}") private val baseUrl: String,
    @Value("\${app.mafiauniverse.authCookie:}") private val authCookie: String
) {
    private val restClient = buildRestClient()

    /**
     * Builds the RestClient with cookie support if configured
     */
    private fun buildRestClient(): RestClient {
        val builder = RestClient.builder().baseUrl(baseUrl)

        if (authCookie.isNotBlank()) {
            builder.defaultHeader("Cookie", ".AspNetCore.Identity.Application=$authCookie")
        }

        return builder.build()
    }

    /**
     * Fetches the tournaments list page HTML
     */
    fun getTournamentsPage(): String {
        return try {
            restClient.get()
                .uri("/Tournaments")
                .retrieve()
                .body(String::class.java) ?: throw RuntimeException("Empty response from tournaments page")
        } catch (e: RestClientException) {
            throw RuntimeException("Failed to fetch tournaments page: ${e.message}", e)
        }
    }

    /**
     * Fetches the games list page HTML for a specific tournament
     */
    fun getGamesListPage(tournamentId: Int): String {
        return try {
            restClient.get()
                .uri("/Tournaments/GamesList/$tournamentId")
                .retrieve()
                .body(String::class.java) ?: throw RuntimeException("Empty response from games list page")
        } catch (e: RestClientException) {
            if (e.message?.contains("404") == true || e.message?.contains("Not Found") == true) {
                throw GameNotFoundException("Tournament $tournamentId not found", e)
            }
            throw RuntimeException("Failed to fetch games list page for tournament $tournamentId: ${e.message}", e)
        }
    }

    /**
     * Fetches the game details page HTML for a specific game
     */
    fun getGameDetailsPage(gameId: Int): String {
        return try {
            restClient.get()
                .uri("/Games/Details/$gameId")
                .retrieve()
                .body(String::class.java) ?: throw RuntimeException("Empty response from game details page")
        } catch (e: RestClientException) {
            if (e.message?.contains("404") == true || e.message?.contains("Not Found") == true) {
                throw GameNotFoundException("Game $gameId not found", e)
            }
            throw RuntimeException("Failed to fetch game details page for game $gameId: ${e.message}", e)
        }
    }

    /**
     * Fetches the tournament players page HTML for a specific tournament
     */
    fun getTournamentPlayersPage(tournamentId: Int): String {
        return try {
            restClient.get()
                .uri("/Tournaments/Players/$tournamentId")
                .retrieve()
                .body(String::class.java) ?: throw RuntimeException("Empty response from tournament players page")
        } catch (e: RestClientException) {
            if (e.message?.contains("404") == true || e.message?.contains("Not Found") == true) {
                throw GameNotFoundException("Tournament $tournamentId not found", e)
            }
            throw RuntimeException(
                "Failed to fetch tournament players page for tournament $tournamentId: ${e.message}",
                e
            )
        }
    }

    /**
     * Custom exception for game not found errors
     */
    class GameNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}
