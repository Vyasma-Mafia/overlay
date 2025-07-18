package com.stoum.overlay.repository

import com.stoum.overlay.entity.PlayerPhoto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerPhotoRepository : JpaRepository<PlayerPhoto, UUID>
