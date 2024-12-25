package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PolemicaUser(
    val id: Long,
    val username: String
)
