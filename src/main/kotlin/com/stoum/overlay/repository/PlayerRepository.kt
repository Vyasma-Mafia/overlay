package com.stoum.overlay.repository

import com.stoum.overlay.entity.Player
import org.springframework.data.repository.CrudRepository
import java.util.*

interface PlayerRepository : CrudRepository<Player, UUID> {
    fun findPlayerByNickname(nickname: String?): Player?
}