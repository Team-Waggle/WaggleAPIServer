package io.waggle.waggleapiserver.domain.post.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.post.Deadline
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import java.time.Instant
import java.time.LocalDate

@Schema(description = "북마크한 모집글 응답 DTO")
data class BookmarkedPostResponse(
    @Schema(description = "모집글 ID", example = "1")
    val id: Long,
    @Schema(description = "모집글 제목", example = "와글에서 기획자 구인합니다")
    val title: String,
    @Schema(description = "작성자 정보")
    val user: UserSimpleResponse,
    @Schema(description = "모집 중 여부")
    val recruiting: Boolean,
    @Schema(description = "모집 정보 목록")
    val recruitments: List<RecruitmentResponse>,
    @Schema(description = "모집 마감일. 그날 24시(KST)까지 모집. 미지정 시 무기한", example = "2026-09-30")
    val deadline: LocalDate?,
    @Schema(description = "모집글 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) : BookmarkResponse {
    companion object {
        fun of(
            post: Post,
            user: UserSimpleResponse,
            recruitments: List<RecruitmentResponse> = emptyList(),
        ): BookmarkedPostResponse =
            BookmarkedPostResponse(
                id = post.id,
                title = post.title,
                user = user,
                recruiting = !post.isExpired && recruitments.any { it.status == RecruitmentStatus.RECRUITING },
                recruitments = recruitments,
                deadline = post.expiresAt?.let { Deadline.toDeadline(it) },
                createdAt = post.createdAt,
            )
    }
}
