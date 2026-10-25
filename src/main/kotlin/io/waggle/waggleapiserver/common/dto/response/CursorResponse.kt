package io.waggle.waggleapiserver.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "커서 기반 페이지네이션 응답 DTO")
data class CursorResponse<T>(
    @Schema(description = "데이터 목록")
    val data: List<T>,
    @Schema(description = "다음 커서 (다음 페이지가 없으면 null)", example = "eyJpZCI6OTV9")
    val nextCursor: String?,
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)
