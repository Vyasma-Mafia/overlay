package com.stoum.overlay.entity

import com.stoum.overlay.entity.converters.MapMapConverter
import com.stoum.overlay.entity.overlay.GamePlayer
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import java.util.*

@Entity
@Table(name = "player")
class Player (
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