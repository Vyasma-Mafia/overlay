package com.stoum.overlay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "facts")
class Fact(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "text", nullable = false, length = 1000)
    var text: String,

    @Column(nullable = true)
    var playerNickname: String? = null,

    @Column(name = "player_photo_url", nullable = true, length = 500)
    var playerPhotoUrl: String? = null,

    @Column(name = "stage_type", nullable = false, length = 100)
    var stageType: String,

    @Column(name = "display_duration_seconds", nullable = false)
    var displayDurationSeconds: Int,

    @Column(name = "is_displayed", nullable = false)
    var isDisplayed: Boolean = false,
)
