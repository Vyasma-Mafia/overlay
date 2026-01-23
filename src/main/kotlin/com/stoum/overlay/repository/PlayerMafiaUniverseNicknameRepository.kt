package com.stoum.overlay.repository

import com.stoum.overlay.entity.PlayerMafiaUniverseNickname
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerMafiaUniverseNicknameRepository : JpaRepository<PlayerMafiaUniverseNickname, UUID> {
    fun findByNickname(nickname: String): PlayerMafiaUniverseNickname?
    fun findByPlayerId(playerId: UUID): List<PlayerMafiaUniverseNickname>
    fun existsByNickname(nickname: String): Boolean
}
