package io.waggle.waggleapiserver.domain.post.repository

import io.waggle.waggleapiserver.domain.post.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PostRepository :
    JpaRepository<Post, Long>,
    PostRepositoryCustom {
    // native UPDATE 직후 같은 트랜잭션에서 읽으려면 엔티티가 아니라 스칼라로 DB 값을 받아야 함
    @Query("SELECT p.likeCount FROM Post p WHERE p.id = :postId")
    fun findLikeCountById(postId: Long): Long?

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
    @Query("UPDATE posts SET like_count = like_count + 1 WHERE id = :postId", nativeQuery = true)
    fun increaseLikeCount(postId: Long)

    @Modifying
    @Query("UPDATE posts SET like_count = like_count - 1 WHERE id = :postId", nativeQuery = true)
    fun decreaseLikeCount(postId: Long)

    // likes 행을 지우기 전에 실행해야 함
    // 지운 뒤에는 그 사용자가 어느 글에 몇 개를 눌렀는지 알 수 없어 카운터가 그대로 남음
    @Modifying
    @Query(
        """
        UPDATE posts p
        JOIN (
            SELECT target_id, COUNT(*) AS liked_count FROM likes
            WHERE type = 'POST' AND user_id = :userId
            GROUP BY target_id
        ) l ON l.target_id = p.id
        SET p.like_count = p.like_count - l.liked_count
        """,
        nativeQuery = true,
    )
    fun decreaseLikeCountByLikerId(userId: UUID)

    @Modifying
    @Query("UPDATE posts SET like_count = 0 WHERE id = :postId", nativeQuery = true)
    fun resetLikeCount(postId: Long)

    @Modifying
    @Query("UPDATE posts SET like_count = 0 WHERE team_id = :teamId", nativeQuery = true)
    fun resetLikeCountByTeamId(teamId: Long)

    @Modifying
    @Query("UPDATE posts SET like_count = 0 WHERE user_id = :userId", nativeQuery = true)
    fun resetLikeCountByUserId(userId: UUID)

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
