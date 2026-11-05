package io.waggle.waggleapiserver.domain.like.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.dto.response.LikeResponse
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LikeService(
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
) {
    @Transactional
    fun like(
        type: LikeType,
        targetId: Long,
        user: User,
    ): LikeResult {
        checkLikableTarget(type, targetId)
        if (type == LikeType.COMMENT && commentRepository.existsByIdAndUserId(targetId, user.id)) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Cannot like your own comment")
        }

        val created = likeRepository.insertIfAbsent(type, targetId, user.id) == 1
        if (created) {
            increaseLikeCount(type, targetId)
        }

        return LikeResult(
            created = created,
            response = LikeResponse.of(liked = true, likeCount = getLikeCount(type, targetId)),
        )
    }

    @Transactional
    fun unlike(
        type: LikeType,
        targetId: Long,
        user: User,
    ): LikeResponse {
        checkLikableTarget(type, targetId)

        val deleted = likeRepository.deleteIfPresent(type, targetId, user.id) == 1
        if (deleted) {
            decreaseLikeCount(type, targetId)
        }

        return LikeResponse.of(liked = false, likeCount = getLikeCount(type, targetId))
    }

    private fun increaseLikeCount(
        type: LikeType,
        targetId: Long,
    ) = when (type) {
        LikeType.POST -> postRepository.increaseLikeCount(targetId)
        LikeType.COMMENT -> commentRepository.increaseLikeCount(targetId)
    }

    private fun decreaseLikeCount(
        type: LikeType,
        targetId: Long,
    ) = when (type) {
        LikeType.POST -> postRepository.decreaseLikeCount(targetId)
        LikeType.COMMENT -> commentRepository.decreaseLikeCount(targetId)
    }

    private fun getLikeCount(
        type: LikeType,
        targetId: Long,
    ): Long =
        when (type) {
            LikeType.POST -> postRepository.findLikeCountById(targetId)
            LikeType.COMMENT -> commentRepository.findLikeCountById(targetId)
        } ?: 0

    private fun checkLikableTarget(
        type: LikeType,
        targetId: Long,
    ) {
        val exists =
            when (type) {
                LikeType.POST -> postRepository.existsById(targetId)
                LikeType.COMMENT -> commentRepository.existsByIdAndTombstonedAtIsNull(targetId)
            }
        if (!exists) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "$type not found: $targetId")
        }
    }
}

data class LikeResult(
    val created: Boolean,
    val response: LikeResponse,
)
