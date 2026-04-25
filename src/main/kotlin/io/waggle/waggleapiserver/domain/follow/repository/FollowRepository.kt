package io.waggle.waggleapiserver.domain.follow.repository

import io.waggle.waggleapiserver.domain.follow.Follow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface FollowRepository : JpaRepository<Follow, Long> {
    fun deleteByFollowerIdAndFolloweeId(
        followerId: UUID,
        followeeId: UUID,
    )

    @Query(
        """
        SELECT * FROM follows
        WHERE follower_id = :followerId AND followee_id = :followeeId
        """,
        nativeQuery = true,
    )
    fun findByFollowerIdAndFolloweeIdIncludingDeleted(
        followerId: UUID,
        followeeId: UUID,
    ): Follow?

    fun countByFollowerId(followerId: UUID): Long

    fun countByFolloweeId(followeeId: UUID): Long

    fun findByFollowerId(followerId: UUID): List<Follow>

    fun findByFolloweeId(followeeId: UUID): List<Follow>

    @Modifying
    @Query(
        """
        UPDATE follows SET deleted_at = UTC_TIMESTAMP(6)
        WHERE (follower_id = :userId OR followee_id = :userId) AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByFollowerIdOrFolloweeIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE follows SET deleted_at = NULL
        WHERE (follower_id = :userId OR followee_id = :userId) AND deleted_at IS NOT NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtNullByFollowerIdOrFolloweeIdAndDeletedAtIsNotNull(userId: UUID)
}
