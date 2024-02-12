package com.stoum.overlay.repository

import com.stoum.overlay.entity.overlay.GamePlayer
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface GamePlayerRepository : CrudRepository<GamePlayer, UUID> {
}