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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

class PolemicaServiceGameTitleTest {

    private lateinit var polemicaService: PolemicaService
    private val gameRepository = mockk<GameRepository>()
    private val polemicaClient = mockk<PolemicaClient>()
    private val emitterService = mockk<EmitterService>()
    private val pointsService = mockk<GamePointsService>()
    private val photoService = mockk<PlayerPhotoService>()
    private val factRepository = mockk<FactRepository>()

    @BeforeEach
    fun setUp() {
        polemicaService = PolemicaService(
            polemicaClient = polemicaClient,
            gameRepository = gameRepository,
            emitterService = emitterService,
            pointsService = pointsService,
            photoService = photoService,
            factRepository = factRepository
        )
    }

    @Test
    fun `generateGameTitle should return correct format for single table`() {
        // Arrange
        val tournamentId = 1
        val phase = 1
        every { gameRepository.findGamesByTournamentId(tournamentId) } returns listOf(
            createMockGame(tournamentId, 1, 1, phase)
        )

        // Act
        val result = ReflectionTestUtils.invokeMethod<String>(
            polemicaService,
            "generateGameTitle",
            "Турнир Весна 2024",
            1,
            1,
            phase,
            tournamentId
        )

        // Assert
        assertEquals("Турнир Весна 2024 | Игра 1", result)
    }

    @Test
    fun `generateGameTitle should return correct format for multiple tables`() {
        // Arrange
        val tournamentId = 1
        val phase = 1
        every { gameRepository.findGamesByTournamentId(tournamentId) } returns listOf(
            createMockGame(tournamentId, 1, 1, phase),
            createMockGame(tournamentId, 1, 2, phase),
            createMockGame(tournamentId, 1, 3, phase)
        )

        // Act
        val result = ReflectionTestUtils.invokeMethod<String>(
            polemicaService,
            "generateGameTitle",
            "Турнир Весна 2024",
            1,
            2,
            phase,
            tournamentId
        )

        // Assert
        assertEquals("Турнир Весна 2024 | Игра 1 | Стол 2", result)
    }

    @Test
    fun `generateGameTitle should return correct format for final with single table`() {
        // Arrange
        val tournamentId = 1
        val phase = 2 // Финал
        every { gameRepository.findGamesByTournamentId(tournamentId) } returns listOf(
            createMockGame(tournamentId, 1, 1, phase)
        )

        // Act
        val result = ReflectionTestUtils.invokeMethod<String>(
            polemicaService,
            "generateGameTitle",
            "Турнир Весна 2024",
            1,
            1,
            phase,
            tournamentId
        )

        // Assert
        assertEquals("Турнир Весна 2024 | Финал | Игра 1", result)
    }

    @Test
    fun `generateGameTitle should return correct format for final with multiple tables`() {
        // Arrange
        val tournamentId = 1
        val phase = 2 // Финал
        every { gameRepository.findGamesByTournamentId(tournamentId) } returns listOf(
            createMockGame(tournamentId, 1, 1, phase),
            createMockGame(tournamentId, 1, 2, phase)
        )

        // Act
        val result = ReflectionTestUtils.invokeMethod<String>(
            polemicaService,
            "generateGameTitle",
            "Турнир Весна 2024",
            1,
            2,
            phase,
            tournamentId
        )

        // Assert
        assertEquals("Турнир Весна 2024 | Финал | Игра 1 | Стол 2", result)
    }

    @Test
    fun `getTablesCountInPhase should return correct count`() {
        // Arrange
        val tournamentId = 1
        val phase = 1
        every { gameRepository.findGamesByTournamentId(tournamentId) } returns listOf(
            createMockGame(tournamentId, 1, 1, phase),
            createMockGame(tournamentId, 1, 2, phase),
            createMockGame(tournamentId, 1, 3, phase),
            createMockGame(tournamentId, 2, 1, phase), // Другая игра, тот же стол
            createMockGame(tournamentId, 1, 1, 2) // Другая фаза
        )

        // Act
        val result = ReflectionTestUtils.invokeMethod<Int>(
            polemicaService,
            "getTablesCountInPhase",
            tournamentId,
            phase
        )

        // Assert
        assertEquals(3, result)
    }

    private fun createMockGame(tournamentId: Int, gameNum: Int, tableNum: Int, phase: Int): Game {
        return Game(
            type = GameType.POLEMICA,
            tournamentId = tournamentId,
            gameNum = gameNum,
            tableNum = tableNum,
            phase = phase
        )
    }
}
