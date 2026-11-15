package io.waggle.waggleapiserver.domain.notification

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Converter
class NotificationMetadataConverter : AttributeConverter<Map<String, Any?>, String> {
    override fun convertToDatabaseColumn(attribute: Map<String, Any?>?): String? =
        attribute?.let { OBJECT_MAPPER.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any?>? =
        dbData?.let { OBJECT_MAPPER.readValue(it, METADATA_TYPE_REFERENCE) }

    companion object {
        private val OBJECT_MAPPER = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        private val METADATA_TYPE_REFERENCE = object : TypeReference<Map<String, Any?>>() {}
    }
}
