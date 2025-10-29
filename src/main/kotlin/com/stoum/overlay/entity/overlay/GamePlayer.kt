package com.stoum.overlay.entity.overlay

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.stoum.overlay.entity.Game
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var checks: MutableList<Map<String, String>>? = arrayListOf(),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var guess: MutableList<Map<String, String>>? = arrayListOf(),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var votedBy: MutableList<Map<String, String>>? = arrayListOf(),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var stat: MutableMap<String, Map<String, String>>? = hashMapOf(),
    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    @JsonBackReference("game-players")
    @JsonIgnore
    var game: Game? = null,
    var customPhoto: Boolean? = null,
    var sourcePlayerId: Long? = null
)
