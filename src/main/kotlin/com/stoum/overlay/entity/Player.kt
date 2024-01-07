package com.stoum.overlay.entity

import com.stoum.overlay.entity.converters.MapListConverter
import com.stoum.overlay.entity.converters.MapMapConverter
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "player")
class Player (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    var nickname: String,
    var place: Int = 0,
    var photoUrl: String? = null,
    var role: String? = null,
    var status: Pair<String, String>? = null,
    @Convert(converter = MapListConverter::class)
    var checks: MutableList<Map<String, String>>? = null,
    @Convert(converter = MapMapConverter::class)
    var stat: MutableMap<String, Map<String, String>>? = null,
/*    @JoinColumn(name = "game", referencedColumnName = "id")
    var gameId: UUID*/
)