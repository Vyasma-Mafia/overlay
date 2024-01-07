package com.stoum.overlay.entity.converters

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter

class MapListConverter : AttributeConverter<List<Map<String, String>>, String> {

    private val objectMapper = ObjectMapper()
    override fun convertToDatabaseColumn(attribute: List<Map<String, String>>?): String {
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<Map<String, String>> {
        return objectMapper.readValue(dbData, object: TypeReference<List<Map<String, String>>>() {})
    }
}