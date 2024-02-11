package com.stoum.overlay.repository.gomafia;

import com.stoum.overlay.entity.gomafia.GomafiaClub
import org.springframework.data.jpa.repository.JpaRepository

interface GomafiaClubRepository : JpaRepository<GomafiaClub, Int> {
}