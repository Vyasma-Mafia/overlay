package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PolemicaCheck(
    val night: Int,
    val role: Role,
    val player: Position
)
