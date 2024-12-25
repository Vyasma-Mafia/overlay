package com.github.mafia.vyasma.polemicaachivementservice.model.game

import jakarta.persistence.Embeddable

@Embeddable
data class PolemicaGamePlace(val clubId: Long? = null, val competitionId: Long? = null) {

}
