package io.waggle.waggleapiserver.common.storage.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.common.storage.ImageContentType
import jakarta.validation.constraints.NotNull

@Schema(description = "Presigned URL 생성 요청 DTO")
data class PresignedUrlRequest(
    @Schema(description = "이미지 Content Type", example = "image/png")
    @field:NotNull
    val contentType: ImageContentType,
)
