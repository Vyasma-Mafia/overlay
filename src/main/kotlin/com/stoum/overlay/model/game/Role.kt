package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.github.mafia.vyasma.polemicaachivementservice.utils.enums.IntEnum
import com.github.mafia.vyasma.polemicaachivementservice.utils.enums.IntEnumDeserializer
import com.github.mafia.vyasma.polemicaachivementservice.utils.enums.IntEnumSerializer

@JsonSerialize(using = IntEnumSerializer::class)
@JsonDeserialize(using = RoleDeserializer::class)
enum class Role(override val value: Int) : IntEnum {
    DON(0),
    MAFIA(1),
    PEACE(2),
    SHERIFF(3)
}

class RoleDeserializer : IntEnumDeserializer<Role>(Role.entries.toTypedArray())
