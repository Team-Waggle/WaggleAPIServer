package io.waggle.waggleapiserver.domain.bookmark.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import jakarta.validation.constraints.NotNull

@Schema(description = "북마크 토글 요청 DTO")
data class BookmarkToggleRequest(
    @Schema(description = "북마크 객체 ID", example = "1")
    @field:NotNull
    val targetId: Long,
    @Schema(description = "북마크 객체 타입", example = "TEAM")
    @field:NotNull
    val type: BookmarkType,
)
