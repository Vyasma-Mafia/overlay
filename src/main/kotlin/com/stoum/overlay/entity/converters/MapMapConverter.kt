package com.stoum.overlay.entity.converters

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter

class MapMapConverter : AttributeConverter<Map<String, Map<String, String>>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Map<String, String>>?): String {
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Map<String, String>> {
        return objectMapper.readValue(dbData, object: TypeReference<Map<String, Map<String, String>>>() {})
    }
}