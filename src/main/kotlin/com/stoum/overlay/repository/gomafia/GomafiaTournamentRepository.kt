package com.stoum.overlay.repository.gomafia;

import com.stoum.overlay.entity.gomafia.GomafiaTournament
import org.springframework.data.jpa.repository.JpaRepository

interface GomafiaTournamentRepository : JpaRepository<GomafiaTournament, Int> {
}