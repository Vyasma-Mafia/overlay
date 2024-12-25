package com.github.mafia.vyasma.polemicaachivementservice.utils.enums

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

interface StringEnum {
    val value: String
}

class StringEnumSerializer<T : StringEnum>() : JsonSerializer<T>() {
    override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.value)
    }
}

open class StringEnumDeserializer<E : StringEnum>(private val enumValues: Array<E>) :
    JsonDeserializer<E>() {

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): E {
        val stringValue = parser.valueAsString
        return enumValues.find { it.value == stringValue }
            ?: throw IllegalArgumentException("Invalid enum value: $stringValue")
    }
}
