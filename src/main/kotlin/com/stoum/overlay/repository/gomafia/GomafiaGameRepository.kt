package com.stoum.overlay.repository.gomafia;

import com.stoum.overlay.entity.gomafia.GomafiaGame
import org.springframework.data.jpa.repository.JpaRepository

interface GomafiaGameRepository : JpaRepository<GomafiaGame, Long> {
}