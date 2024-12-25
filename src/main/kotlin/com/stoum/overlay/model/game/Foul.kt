package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Foul(
    val time: LocalDateTime,
    val stage: Stage
)
