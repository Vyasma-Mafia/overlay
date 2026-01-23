package com.stoum.overlay.service

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.stoum.overlay.entity.Player
import com.stoum.overlay.repository.GamePlayerRepository
import com.stoum.overlay.repository.PlayerMafiaUniverseNicknameRepository
import com.stoum.overlay.repository.PlayerRepository
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class PlayerServiceTest {

    private lateinit var playerRepository: PlayerRepository
    private lateinit var gamePlayerRepository: GamePlayerRepository
    private lateinit var playerService: PlayerService

    @BeforeEach
    fun setup() {
        playerRepository = mockk()
        gamePlayerRepository = mockk(relaxed = true)
        val gomafiaRestClient: GomafiaRestClient = mockk(relaxed = true)
        val polemicaClient: PolemicaClient = mockk(relaxed = true)
        val playerMafiaUniverseNicknameRepository: PlayerMafiaUniverseNicknameRepository = mockk(relaxed = true)
        playerService = PlayerService(
            playerRepository,
            gamePlayerRepository,
            gomafiaRestClient,
            polemicaClient,
            playerMafiaUniverseNicknameRepository
        )
    }

    @Test
    fun `searchPlayers should return empty list for blank query`() {
        // When
        val result = playerService.searchPlayers("")

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 0) { playerRepository.searchPlayers(any(), any(), any()) }
    }

    @Test
    fun `searchPlayers should call repository with correct parameters`() {
        // Given
        val query = "TestPlayer"
        val expectedPlayers = listOf(
            Player(id = UUID.randomUUID(), nickname = "TestPlayer", polemicaId = 123L, gomafiaId = 456L)
        )
        every { playerRepository.searchPlayers(query, null, null) } returns expectedPlayers

        // When
        val result = playerService.searchPlayers(query)

        // Then
        assertEquals(expectedPlayers, result)
        verify { playerRepository.searchPlayers(query, null, null) }
    }

    @Test
    fun `searchPlayers should parse numeric query as IDs`() {
        // Given
        val query = "12345"
        val expectedPlayers = listOf(
            Player(id = UUID.randomUUID(), nickname = "Player1", polemicaId = 12345L, gomafiaId = null)
        )
        every { playerRepository.searchPlayers(query, 12345L, 12345L) } returns expectedPlayers

        // When
        val result = playerService.searchPlayers(query)

        // Then
        assertEquals(expectedPlayers, result)
        verify { playerRepository.searchPlayers(query, 12345L, 12345L) }
    }

    @Test
    fun `checkForDuplicateIds should detect duplicate Polemica ID`() {
        // Given
        val playerId = UUID.randomUUID()
        val duplicatePlayer = Player(id = UUID.randomUUID(), nickname = "Duplicate", polemicaId = 123L)
        every { playerRepository.findByPolemicaIdAndIdNot(123L, playerId) } returns duplicatePlayer
        every { playerRepository.findByGomafiaIdAndIdNot(any(), any()) } returns null

        // When
        val result = playerService.checkForDuplicateIds(playerId, 123L, null)

        // Then
        assertTrue(result.hasDuplicates)
        assertEquals(duplicatePlayer, result.duplicatePolemicaPlayer)
        assertNull(result.duplicateGomafiaPlayer)
    }

    @Test
    fun `checkForDuplicateIds should detect duplicate Gomafia ID`() {
        // Given
        val playerId = UUID.randomUUID()
        val duplicatePlayer = Player(id = UUID.randomUUID(), nickname = "Duplicate", gomafiaId = 456L)
        every { playerRepository.findByPolemicaIdAndIdNot(any(), any()) } returns null
        every { playerRepository.findByGomafiaIdAndIdNot(456L, playerId) } returns duplicatePlayer

        // When
        val result = playerService.checkForDuplicateIds(playerId, null, 456L)

        // Then
        assertTrue(result.hasDuplicates)
        assertNull(result.duplicatePolemicaPlayer)
        assertEquals(duplicatePlayer, result.duplicateGomafiaPlayer)
    }

    @Test
    fun `checkForDuplicateIds should return no duplicates when IDs are unique`() {
        // Given
        val playerId = UUID.randomUUID()
        every { playerRepository.findByPolemicaIdAndIdNot(any(), any()) } returns null
        every { playerRepository.findByGomafiaIdAndIdNot(any(), any()) } returns null

        // When
        val result = playerService.checkForDuplicateIds(playerId, 123L, 456L)

        // Then
        assertFalse(result.hasDuplicates)
        assertNull(result.duplicatePolemicaPlayer)
        assertNull(result.duplicateGomafiaPlayer)
    }
}
