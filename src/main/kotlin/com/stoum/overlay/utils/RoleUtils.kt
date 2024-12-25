package com.github.mafia.vyasma.polemicaachivementservice.utils

import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role

fun Role.isRed(): Boolean {
    return when (this) {
        Role.SHERIFF, Role.PEACE -> true
        Role.DON, Role.MAFIA -> false
    }
}

fun Role.isBlack() = isRed().not()
