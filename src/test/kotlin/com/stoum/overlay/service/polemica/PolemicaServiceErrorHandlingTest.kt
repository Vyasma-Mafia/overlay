package com.stoum.overlay.service.polemica

import com.github.mafia.vyasma.polemica.library.client.GamePointsService
import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.enums.GameType
import com.stoum.overlay.repository.FactRepository
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.PlayerPhotoService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class PolemicaServiceErrorHandlingTest {

    private lateinit var polemicaService: PolemicaService
    private val polemicaClient = mockk<PolemicaClient>()
    private val gameRepository = mockk<GameRepository>()
    private val emitterService = mockk<EmitterService>()
    private val pointsService = mockk<GamePointsService>()
    private val photoService = mockk<PlayerPhotoService>()
    private val factRepository = mockk<FactRepository>()

    @BeforeEach
    fun setUp() {
        polemicaService = PolemicaService(
            polemicaClient,
            gameRepository,
            factRepository,
            emitterService,
            pointsService,
            photoService,
        )
    }

    @Test
    fun `should restart game crawling successfully`() {
        // Given
        val gameId = UUID.randomUUID()
        val game = Game(
            id = gameId,
            type = GameType.POLEMICA,
            tournamentId = 1,
            gameNum = 1,
            tableNum = 1,
            phase = 1,
            started = false,
            crawlFailureCount = 3,
            lastCrawlError = "Network error",
            crawlStopReason = "NETWORK_ERRORS"
        )

        every { gameRepository.findById(gameId) } returns Optional.of(game)
        val savedGameSlot = slot<Game>()
        every { gameRepository.save(capture(savedGameSlot)) } returns game

        // When
        val result = polemicaService.restartGameCrawling(gameId)

        // Then
        assertTrue(result)
        verify { gameRepository.save(any()) }

        val savedGame = savedGameSlot.captured
        assertNull(savedGame.crawlFailureCount)
        assertNull(savedGame.lastCrawlError)
        assertNull(savedGame.lastFailureTime)
        assertNull(savedGame.crawlStopReason)
        assertTrue(savedGame.started == true)
    }

    @Test
    fun `should return false when game not found for restart`() {
        // Given
        val gameId = UUID.randomUUID()
        every { gameRepository.findById(gameId) } returns Optional.empty()

        // When
        val result = polemicaService.restartGameCrawling(gameId)

        // Then
        assertFalse(result)
        verify(exactly = 0) { gameRepository.save(any()) }
    }

    @Test
    fun `should get crawl error statistics correctly`() {
        // Given
        val stoppedGames = listOf(
            Game(
                type = GameType.POLEMICA,
                started = false,
                crawlStopReason = "GAME_DELETED_IN_POLEMICA",
                lastFailureTime = LocalDateTime.now().minusHours(2)
            ),
            Game(
                type = GameType.POLEMICA,
                started = false,
                crawlStopReason = "NETWORK_ERRORS",
                lastFailureTime = LocalDateTime.now().minusHours(12)
            ),
            Game(
                type = GameType.POLEMICA,
                started = false,
                crawlStopReason = "NETWORK_ERRORS",
                lastFailureTime = LocalDateTime.now().minusHours(30)
            )
        )

        every { gameRepository.findGameByTypeAndStarted(GameType.POLEMICA, false) } returns stoppedGames

        // When
        val statistics = polemicaService.getCrawlErrorStatistics()

        // Then
        assertEquals(3, statistics["totalStopped"])
        val byReason = statistics["byReason"] as Map<*, *>
        assertEquals(1, byReason["GAME_DELETED_IN_POLEMICA"])
        assertEquals(2, byReason["NETWORK_ERRORS"])
        assertEquals(2, statistics["recentFailures"]) // За последние 24 часа
    }

    @Test
    fun `should get problematic games list correctly`() {
        // Given
        val problematicGames = listOf(
            Game(
                id = UUID.randomUUID(),
                type = GameType.POLEMICA,
                tournamentId = 1,
                gameNum = 1,
                tableNum = 1,
                started = false,
                crawlFailureCount = 2,
                lastCrawlError = "Network timeout",
                crawlStopReason = "NETWORK_ERRORS"
            )
        )

        every {
            gameRepository.findGamesByTypeAndStartedAndCrawlFailureCountGreaterThan(
                GameType.POLEMICA, false, 0
            )
        } returns problematicGames

        // When
        val result = polemicaService.getProblematicGames()

        // Then
        assertEquals(1, result.size)
        val gameInfo = result[0]
        assertEquals(problematicGames[0].id, gameInfo["id"])
        assertEquals(1, gameInfo["tournamentId"])
        assertEquals(1, gameInfo["gameNum"])
        assertEquals(1, gameInfo["tableNum"])
        assertEquals(2, gameInfo["failureCount"])
        assertEquals("Network timeout", gameInfo["lastError"])
        assertEquals("NETWORK_ERRORS", gameInfo["stopReason"])
    }

    @Test
    fun `should auto recover stopped games after timeout`() {
        // Given
        val oldFailureTime = LocalDateTime.now().minusHours(2)
        val recentFailureTime = LocalDateTime.now().minusMinutes(30)

        val recoverableGame = Game(
            id = UUID.randomUUID(),
            type = GameType.POLEMICA,
            started = false,
            crawlStopReason = "NETWORK_ERRORS",
            lastFailureTime = oldFailureTime
        )

        val nonRecoverableGame = Game(
            id = UUID.randomUUID(),
            type = GameType.POLEMICA,
            started = false,
            crawlStopReason = "GAME_DELETED_IN_POLEMICA",
            lastFailureTime = oldFailureTime
        )

        val recentFailureGame = Game(
            id = UUID.randomUUID(),
            type = GameType.POLEMICA,
            started = false,
            crawlStopReason = "NETWORK_ERRORS",
            lastFailureTime = recentFailureTime
        )

        val stoppedGames = listOf(recoverableGame, nonRecoverableGame, recentFailureGame)

        every {
            gameRepository.findGamesByTypeAndStartedAndCrawlStopReasonIsNotNull(
                GameType.POLEMICA, false
            )
        } returns stoppedGames

        every { gameRepository.findById(recoverableGame.id!!) } returns Optional.of(recoverableGame)
        every { gameRepository.save(any()) } returns recoverableGame

        // When
        polemicaService.autoRecoverStoppedGames()

        // Then
        verify(exactly = 1) { gameRepository.save(any()) } // Только одна игра должна быть восстановлена
    }
}
