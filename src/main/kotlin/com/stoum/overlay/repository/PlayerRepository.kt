package com.stoum.overlay.repository

import com.stoum.overlay.entity.Player
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface PlayerRepository : CrudRepository<Player, UUID> {
}