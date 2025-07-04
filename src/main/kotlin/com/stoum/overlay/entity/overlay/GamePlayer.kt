package com.stoum.overlay.entity.overlay

import com.stoum.overlay.entity.converters.MapListConverter
import com.stoum.overlay.entity.converters.MapMapConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "game_player")
data class GamePlayer(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    var nickname: String,
    var place: Int = 1,
    var photoUrl: String? = null,
    var role: String? = null,
    var status: String? = null,
    var fouls: Int? = 0,
    var techs: Int? = 0,
    var speaker: Boolean? = false,
    var voting: Boolean? = false,
    var clubIcon: String? = null,
    var score: Double? = null,
    @Convert(converter = MapListConverter::class)
    var checks: MutableList<Map<String, String>>? = arrayListOf(),
    @Convert(converter = MapListConverter::class)
    var guess: MutableList<Map<String, String>>? = arrayListOf(),
    @Convert(converter = MapMapConverter::class)
    var stat: MutableMap<String, Map<String, String>>? = hashMapOf(),
    var customPhoto: Boolean? = null,
    var sourcePlayerId: Long? = null
/*    @JoinColumn(name = "game", referencedColumnName = "id")
    var gameId: UUID*/
)
