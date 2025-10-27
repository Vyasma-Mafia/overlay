package com.stoum.overlay.repository

import com.stoum.overlay.entity.Fact
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FactRepository : JpaRepository<Fact, UUID> {
}
