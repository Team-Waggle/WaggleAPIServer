package io.waggle.waggleapiserver.domain.post.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse

@Schema(description = "모집글 응답 DTO")
data class PostSimpleResponse(
    @Schema(description = "모집글 ID", example = "1")
    val postId: Long,
    @Schema(description = "모집글 제목", example = "와글에서 기획자 구인합니다")
    val title: String,
    @Schema(description = "모집글 내용")
    val content: String,
    @Schema(description = "작성자 정보")
    val user: UserSimpleResponse,
) : BookmarkResponse {
    companion object {
        fun of(
            post: Post,
            userSimpleResponse: UserSimpleResponse,
        ): PostSimpleResponse =
            PostSimpleResponse(
                postId = post.id,
                title = post.title,
                content = post.content,
                user = userSimpleResponse,
            )
    }
}
