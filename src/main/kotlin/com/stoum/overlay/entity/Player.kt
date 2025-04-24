package com.stoum.overlay.entity

import com.stoum.overlay.entity.converters.MapMapConverter
import jakarta.persistence.CascadeType
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "player")
data class Player(
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        var id: UUID? = null,
        var nickname: String,
        @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
        @JoinColumn(name = "player_id")
        var playerPhotos: MutableList<PlayerPhoto> = mutableListOf(),
        @Convert(converter = MapMapConverter::class)
        var stat: MutableMap<String, Map<String, String>>? = null,
)
