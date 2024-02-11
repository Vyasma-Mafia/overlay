package com.stoum.overlay.repository.gomafia;

import com.stoum.overlay.entity.gomafia.GomafiaTournamentResult
import org.springframework.data.jpa.repository.JpaRepository

interface GomafiaTournamentResultRepository : JpaRepository<GomafiaTournamentResult, Long> {
}