package io.waggle.waggleapiserver.domain.post.repository

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.util.Cursor
import io.waggle.waggleapiserver.common.util.CursorCodec
import io.waggle.waggleapiserver.domain.post.PostSort
import java.time.Instant

// 선언 순서가 곧 노출 순서임
enum class DeadlineGroup {
    EXPIRING,
    INDEFINITE,
    CLOSED,
}

// 정렬 키를 커서가 직접 들고 있어야 커서 글이 삭제되거나 조회수, 좋아요 수가 바뀌어도 이어붙일 수 있음
sealed interface PostCursor : Cursor {
    val id: Long

    data class Id(
        override val id: Long,
    ) : PostCursor

    data class ViewCount(
        val viewCount: Long,
        override val id: Long,
    ) : PostCursor

    data class LikeCount(
        val likeCount: Long,
        override val id: Long,
    ) : PostCursor

    data class Deadline(
        val group: DeadlineGroup,
        val expiresAt: Instant?,
        override val id: Long,
    ) : PostCursor

    companion object {
        fun decode(
            cursor: String,
            sort: PostSort,
        ): PostCursor =
            when (sort) {
                PostSort.NEWEST, PostSort.OLDEST ->
                    CursorCodec.decodeLegacyId(cursor)?.let { Id(it) }
                        ?: CursorCodec.decode(cursor, Id::class.java)

                PostSort.MOST_VIEWED -> CursorCodec.decode(cursor, ViewCount::class.java)

                PostSort.MOST_LIKED -> CursorCodec.decode(cursor, LikeCount::class.java)

                PostSort.DEADLINE_SOON ->
                    CursorCodec.decode(cursor, Deadline::class.java).also {
                        // EXPIRING 묶음은 expires_at으로 이어붙이므로 값이 없으면 깨진 커서임
                        if (it.group == DeadlineGroup.EXPIRING && it.expiresAt == null) {
                            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Invalid cursor")
                        }
                    }
            }
    }
}
