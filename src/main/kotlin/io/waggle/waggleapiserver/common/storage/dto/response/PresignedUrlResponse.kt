package io.waggle.waggleapiserver.common.storage.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.common.storage.GeneratedUploadUrl

@Schema(description = "Presigned URL 생성 응답 DTO")
data class PresignedUrlResponse(
    @Schema(description = "S3 업로드용 Presigned URL")
    val presignedUrl: String,
    @Schema(description = "업로드 완료 후 접근 가능한 Object URL")
    val objectUrl: String,
) {
    companion object {
        fun from(generatedUploadUrl: GeneratedUploadUrl) =
            PresignedUrlResponse(
                presignedUrl = generatedUploadUrl.presignedUrl,
                objectUrl = generatedUploadUrl.objectUrl,
            )
    }
}
