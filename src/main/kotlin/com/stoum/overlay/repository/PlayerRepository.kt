package com.stoum.overlay.repository

import com.stoum.overlay.entity.Player
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerRepository : JpaRepository<Player, UUID> {
    fun findPlayerByNickname(nickname: String?): Player?
}
