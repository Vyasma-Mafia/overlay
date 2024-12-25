package com.github.mafia.vyasma.polemicaachivementservice.utils.enums

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

interface IntEnum {
    val value: Int
}

// Generic Enum Serializer for any enum class having a 'value' property of type Int
class IntEnumSerializer<T : IntEnum>() : JsonSerializer<T>() {
    override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(value.value)
    }
}

// Generic Enum Deserializer for any enum class having a 'value' property of type Int
open class IntEnumDeserializer<E : IntEnum>(private val enumValues: Array<E>) :
    JsonDeserializer<E>() {

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): E {
        val intValue = parser.intValue
        return enumValues.find { it.value == intValue }
            ?: throw IllegalArgumentException("Invalid enum value: $intValue")
    }
}
