package io.waggle.waggleapiserver.domain.bookmark.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "북마크")
@RequestMapping("/posts/{postId}/bookmark")
@RestController
class PostBookmarkController(
    private val bookmarkService: BookmarkService,
) {
    @Operation(
        summary = "모집글 북마크",
        responses = [
            ApiResponse(responseCode = "201", description = "북마크 생성"),
            ApiResponse(responseCode = "204", description = "이미 북마크한 상태"),
        ],
    )
    @PutMapping
    fun bookmarkPost(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ): ResponseEntity<Unit> {
        val created = bookmarkService.bookmark(BookmarkType.POST, postId, user)
        return ResponseEntity.status(if (created) HttpStatus.CREATED else HttpStatus.NO_CONTENT).build()
    }

    @Operation(summary = "모집글 북마크 취소")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unbookmarkPost(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ) = bookmarkService.unbookmark(BookmarkType.POST, postId, user)
}
