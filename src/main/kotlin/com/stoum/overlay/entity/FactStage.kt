package com.stoum.overlay.entity

import com.github.mafia.vyasma.polemica.library.model.game.Stage
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class FactStage(
    @Column(name = "stage_type", nullable = false, length = 50)
    val type: String,

    @Column(name = "stage_day", nullable = true)
    val day: Int? = null,

    @Column(name = "stage_player", nullable = true)
    val player: Int? = null,

    @Column(name = "stage_voting", nullable = true)
    val voting: Int? = null
) : Comparable<FactStage> {

    override fun compareTo(other: FactStage): Int {
        // Порядок сравнения: type -> day -> player -> voting
        val typeComparison = getTypeOrder().compareTo(other.getTypeOrder())
        if (typeComparison != 0) return typeComparison

        val dayComparison = (day ?: 0).compareTo(other.day ?: 0)
        if (dayComparison != 0) return dayComparison

        val playerComparison = (player ?: 0).compareTo(other.player ?: 0)
        if (playerComparison != 0) return playerComparison

        return (voting ?: 0).compareTo(other.voting ?: 0)
    }

    private fun getTypeOrder(): Int = when (type) {
        "dealing" -> 1
        "briefing" -> 2
        "comIntro" -> 3
        "speech" -> 4
        "voting" -> 5
        "voted" -> 6
        "shooting" -> 7
        "shooted" -> 8
        "respeech" -> 9
        "lift" -> 10
        "donCheck" -> 11
        "comCheck" -> 12
        "guess" -> 13
        "comKill" -> 14
        "gameOver" -> 15
        else -> 999
    }

    companion object {
        fun fromPolemicaStage(stage: Stage): FactStage {
            return FactStage(
                type = stage.type.value,
                day = stage.day,
                player = stage.player,
                voting = stage.voting
            )
        }
    }
}
