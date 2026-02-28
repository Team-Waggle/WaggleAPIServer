package io.waggle.waggleapiserver.common.storage

import com.fasterxml.jackson.annotation.JsonCreator
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode

enum class ImageContentType(
    val mediaType: String,
    val extension: String,
) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    ;

    companion object {
        private val MEDIA_TYPE_MAP = entries.associateBy { it.mediaType }

        @JvmStatic
        @JsonCreator
        fun from(mediaType: String): ImageContentType =
            MEDIA_TYPE_MAP[mediaType]
                ?: throw BusinessException(ErrorCode.INVALID_TYPE_VALUE, "Unsupported image type: $mediaType")
    }
}
