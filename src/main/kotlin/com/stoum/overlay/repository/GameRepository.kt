package com.stoum.overlay.repository

import com.stoum.overlay.entity.Game
import org.springframework.data.repository.CrudRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Component
import java.util.UUID

interface GameRepository : CrudRepository<Game, UUID> {
}