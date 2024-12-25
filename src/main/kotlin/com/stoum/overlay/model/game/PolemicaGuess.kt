package com.github.mafia.vyasma.polemicaachivementservice.model.game

data class PolemicaGuess(
    val civs: List<Position>?,
    val mafs: List<Position>?,
    val vice: Position?
) {
}
