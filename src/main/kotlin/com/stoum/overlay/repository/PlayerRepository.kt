package com.stoum.overlay.repository

import com.stoum.overlay.entity.Player
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PlayerRepository : JpaRepository<Player, UUID> {
    fun findPlayerByNickname(nickname: String?): Player?
    fun findPlayerByPolemicaId(polemicaId: Long): Player?
    fun findPlayerByGomafiaId(gomafiaId: Long): Player?

    // Поиск игроков по никнейму (частичное совпадение) или точному ID
    @Query(
        """
        SELECT p FROM Player p
        WHERE LOWER(p.nickname) LIKE LOWER(CONCAT('%', :query, '%'))
           OR (CAST(:polemicaId AS long) IS NOT NULL AND p.polemicaId = :polemicaId)
           OR (CAST(:gomafiaId AS long) IS NOT NULL AND p.gomafiaId = :gomafiaId)
    """
    )
    fun searchPlayers(
        @Param("query") query: String,
        @Param("polemicaId") polemicaId: Long?,
        @Param("gomafiaId") gomafiaId: Long?
    ): List<Player>

    // Проверка существования игрока с данным Polemica ID (исключая определенного игрока)
    fun existsByPolemicaIdAndIdNot(polemicaId: Long, id: UUID): Boolean

    // Проверка существования игрока с данным Gomafia ID (исключая определенного игрока)
    fun existsByGomafiaIdAndIdNot(gomafiaId: Long, id: UUID): Boolean

    // Поиск игрока по Polemica ID (исключая определенного игрока)
    fun findByPolemicaIdAndIdNot(polemicaId: Long, id: UUID): Player?

    // Поиск игрока по Gomafia ID (исключая определенного игрока)
    fun findByGomafiaIdAndIdNot(gomafiaId: Long, id: UUID): Player?
}
