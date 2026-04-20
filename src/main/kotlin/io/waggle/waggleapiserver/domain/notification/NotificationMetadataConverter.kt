package io.waggle.waggleapiserver.domain.notification

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class NotificationMetadataConverter : AttributeConverter<Map<String, Any?>, String> {
    override fun convertToDatabaseColumn(attribute: Map<String, Any?>?): String? =
        attribute?.let { OBJECT_MAPPER.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any?>? =
        dbData?.let { OBJECT_MAPPER.readValue(it, METADATA_TYPE_REFERENCE) }

    companion object {
        private val OBJECT_MAPPER = ObjectMapper().registerModule(KotlinModule.Builder().build())
        private val METADATA_TYPE_REFERENCE = object : TypeReference<Map<String, Any?>>() {}
    }
}
