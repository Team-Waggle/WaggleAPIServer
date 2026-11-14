package io.waggle.waggleapiserver.domain.follow.repository

import io.waggle.waggleapiserver.domain.follow.Follow
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface FollowRepository : JpaRepository<Follow, Long> {
    // 재가입 직후 프로필을 설정하지 않은 사용자는 username이 없어 목록 응답을 만들 수 없음
    // 수도 목록과 같은 기준으로 세야 표시된 수와 목록이 어긋나지 않음
    @Query(
        """
        SELECT COUNT(f) FROM Follow f JOIN User u ON u.id = f.followeeId
        WHERE f.followerId = :followerId AND u.username IS NOT NULL AND u.position IS NOT NULL
        """,
    )
    fun countFolloweesWithCompleteProfile(followerId: UUID): Long

    @Query(
        """
        SELECT COUNT(f) FROM Follow f JOIN User u ON u.id = f.followerId
        WHERE f.followeeId = :followeeId AND u.username IS NOT NULL AND u.position IS NOT NULL
        """,
    )
    fun countFollowersWithCompleteProfile(followeeId: UUID): Long

    @Query(
        """
        SELECT COUNT(*) FROM follows
        WHERE follower_id = :followerId AND followee_id = :followeeId AND deleted_at IS NOT NULL
        """,
        nativeQuery = true,
    )
    fun countByFollowerIdAndFolloweeIdAndDeletedAtIsNotNull(
        followerId: UUID,
        followeeId: UUID,
    ): Long

    @Query(
        """
        SELECT u FROM Follow f JOIN User u ON u.id = f.followeeId
        WHERE f.followerId = :followerId AND u.username IS NOT NULL AND u.position IS NOT NULL
        ORDER BY f.id DESC
        """,
    )
    fun findFolloweesWithCompleteProfile(followerId: UUID): List<User>

    @Query(
        """
        SELECT u FROM Follow f JOIN User u ON u.id = f.followerId
        WHERE f.followeeId = :followeeId AND u.username IS NOT NULL AND u.position IS NOT NULL
        ORDER BY f.id DESC
        """,
    )
    fun findFollowersWithCompleteProfile(followeeId: UUID): List<User>

    // 동시 PUT이 확인 후 저장을 함께 통과하면 유니크 충돌로 500이 되므로 INSERT 하나로 판정
    @Modifying
    @Query(
        """
        INSERT IGNORE INTO follows (follower_id, followee_id, created_at, updated_at)
        VALUES (:followerId, :followeeId, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        followerId: UUID,
        followeeId: UUID,
    ): Int

    fun deleteByFollowerIdAndFolloweeId(
        followerId: UUID,
        followeeId: UUID,
    )

    @Modifying
    @Query(
        """
        UPDATE follows SET deleted_at = NULL, updated_at = UTC_TIMESTAMP(6)
        WHERE follower_id = :followerId AND followee_id = :followeeId AND deleted_at IS NOT NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtNullByFollowerIdAndFolloweeIdAndDeletedAtIsNotNull(
        followerId: UUID,
        followeeId: UUID,
    ): Int

    @Modifying
    @Query(
        """
        UPDATE follows SET deleted_at = UTC_TIMESTAMP(6)
        WHERE (follower_id = :userId OR followee_id = :userId) AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByFollowerIdOrFolloweeIdAndDeletedAtIsNull(userId: UUID)

    // 상대가 탈퇴 상태인 팔로우까지 되살리면 조회되지 않는 사용자를 가리키게 됨
    // 상대가 나중에 재가입하면 그때 이 쿼리로 되살아남
    @Modifying
    @Query(
        """
        UPDATE follows f
        JOIN users partner
            ON partner.id = IF(f.follower_id = :userId, f.followee_id, f.follower_id)
        SET f.deleted_at = NULL
        WHERE (f.follower_id = :userId OR f.followee_id = :userId)
        AND f.deleted_at IS NOT NULL
        AND partner.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtNullByUserIdAndPartnerActive(userId: UUID)
}
