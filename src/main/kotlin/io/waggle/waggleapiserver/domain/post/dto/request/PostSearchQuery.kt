package io.waggle.waggleapiserver.domain.post.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "모집글 검색 쿼리 DTO")
data class PostSearchQuery(
    @Schema(description = "검색 쿼리")
    val query: String,
)
