package io.waggle.waggleapiserver.domain.post

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "모집글 정렬 기준")
enum class PostSort {
    @Schema(description = "최신순")
    NEWEST,

    @Schema(description = "오래된 순")
    OLDEST,

    @Schema(description = "좋아요순")
    MOST_LIKED,

    @Schema(description = "조회순")
    MOST_VIEWED,

    @Schema(description = "마감 임박순")
    DEADLINE_SOON,
}
