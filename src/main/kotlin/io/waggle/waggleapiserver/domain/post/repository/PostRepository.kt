package io.waggle.waggleapiserver.domain.post.repository

import io.waggle.waggleapiserver.domain.post.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PostRepository :
    JpaRepository<Post, Long>,
    PostRepositoryCustom {
    fun findByIdInOrderByIdDesc(ids: List<Long>): List<Post>

    fun findByTeamIdOrderByIdDesc(teamId: Long): List<Post>

    // 엔티티 dirty checking을 쓰면 @LastModifiedDate가 updated_at을 갱신해 전 모집글이 수정된 것처럼 보임
    @Modifying
    @Query(
        """
        UPDATE posts SET view_count = view_count + :delta
        WHERE id = :postId
        """,
        nativeQuery = true,
    )
    fun increaseViewCount(
        postId: Long,
        delta: Long,
    )

    @Modifying
    @Query(
        """
        UPDATE posts SET deleted_at = UTC_TIMESTAMP(6)
        WHERE user_id = :userId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE posts SET deleted_at = UTC_TIMESTAMP(6)
        WHERE team_id = :teamId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByTeamIdAndDeletedAtIsNull(teamId: Long)
}
