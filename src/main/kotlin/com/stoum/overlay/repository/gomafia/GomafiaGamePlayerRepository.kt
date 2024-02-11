package com.stoum.overlay.repository.gomafia;

import com.stoum.overlay.entity.gomafia.GomafiaGamePlayer
import org.springframework.data.jpa.repository.JpaRepository

interface GomafiaGamePlayerRepository : JpaRepository<GomafiaGamePlayer, Long> {
}