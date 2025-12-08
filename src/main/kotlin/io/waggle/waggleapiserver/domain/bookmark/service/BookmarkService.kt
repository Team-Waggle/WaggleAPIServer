package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.request.BookmarkToggleRequest
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkToggleResponse
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val postRepository: PostRepository,
    private val projectRepository: ProjectRepository,
) {
    fun toggleBookmark(
        request: BookmarkToggleRequest,
        user: User,
    ): BookmarkToggleResponse {
        val (targetId, type) = request

        val bookmarkId =
            BookmarkId(
                userId = user.id,
                targetId = targetId,
                type = type,
            )
        return if (bookmarkRepository.existsById(bookmarkId)) {
            bookmarkRepository.deleteById(bookmarkId)
            BookmarkToggleResponse(false)
        } else {
            val bookmark = Bookmark(bookmarkId)
            bookmarkRepository.save(bookmark)
            BookmarkToggleResponse(true)
        }
    }

    fun getUserBookmarkables(
        type: BookmarkType,
        user: User,
    ): List<BookmarkResponse> {
        val targetIds =
            bookmarkRepository
                .findByIdUserIdAndIdType(user.id, type)
                .map { it.targetId }

        return when (type) {
            BookmarkType.POST -> {
                postRepository
                    .findByIdInOrderByCreatedAtDesc(targetIds)
                    .map { PostSimpleResponse.of(it, UserSimpleResponse.from(user)) }
            }

            BookmarkType.PROJECT -> {
                projectRepository
                    .findByIdInOrderByCreatedAtDesc(targetIds)
                    .map { ProjectSimpleResponse.from(it) }
            }
        }
    }
}
