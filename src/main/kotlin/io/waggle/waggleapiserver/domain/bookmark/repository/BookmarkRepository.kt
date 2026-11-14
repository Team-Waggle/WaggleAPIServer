package io.waggle.waggleapiserver.domain.bookmark.repository

import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BookmarkRepository : JpaRepository<Bookmark, BookmarkId> {
    fun countByIdTargetIdAndIdType(
        targetId: Long,
        type: BookmarkType,
    ): Int

    fun findByIdUserIdAndIdType(
        userId: UUID,
        type: BookmarkType,
    ): List<Bookmark>

    // 동시 PUT이 확인 후 저장을 함께 통과하면 PK 충돌로 500이 되므로 INSERT 하나로 판정
    @Modifying
    @Query(
        """
        INSERT IGNORE INTO bookmarks (type, target_id, user_id, created_at)
        VALUES (:#{#type.name()}, :targetId, :userId, UTC_TIMESTAMP(6))
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        type: BookmarkType,
        targetId: Long,
        userId: UUID,
    ): Int

    @Modifying
    @Query(
        """
        DELETE FROM bookmarks
        WHERE type = :#{#type.name()} AND target_id = :targetId AND user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteIfPresent(
        type: BookmarkType,
        targetId: Long,
        userId: UUID,
    ): Int

    fun deleteByIdUserId(userId: UUID)

    fun deleteByIdTargetIdAndIdType(
        targetId: Long,
        type: BookmarkType,
    )

    @Modifying
    @Query(
        """
        DELETE b FROM bookmarks b
        JOIN posts p ON p.id = b.target_id
        WHERE b.type = 'POST' AND p.team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByPostTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE b FROM bookmarks b
        JOIN posts p ON p.id = b.target_id
        WHERE b.type = 'POST' AND p.user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteByPostUserId(userId: UUID)
}
