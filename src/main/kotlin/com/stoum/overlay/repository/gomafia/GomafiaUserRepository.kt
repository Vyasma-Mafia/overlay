package com.stoum.overlay.repository.gomafia;

import com.stoum.overlay.entity.gomafia.GomafiaUser
import org.springframework.data.jpa.repository.JpaRepository

interface GomafiaUserRepository : JpaRepository<GomafiaUser, Int> {
    fun getByLogin(login: String): GomafiaUser
}