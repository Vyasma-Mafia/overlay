package com.stoum.overlay.repository

import com.stoum.overlay.entity.overlay.GamePlayer
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GamePlayerRepository : JpaRepository<GamePlayer, UUID> {
    fun findBySourcePlayerId(sourcePlayerId: Long): List<GamePlayer>
}
