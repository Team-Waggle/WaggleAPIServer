package io.waggle.waggleapiserver.domain.application.repository

import io.waggle.waggleapiserver.domain.application.ApplicationRead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ApplicationReadRepository : JpaRepository<ApplicationRead, Long> {
    @Query(
        """
        SELECT ar.applicationId FROM ApplicationRead ar
        WHERE ar.userId = :userId
        AND ar.applicationId IN :applicationIds
        """,
    )
    fun findReadApplicationIds(
        userId: UUID,
        applicationIds: List<Long>,
    ): List<Long>

    fun findByApplicationIdInAndUserIdIn(
        applicationIds: List<Long>,
        userIds: List<UUID>,
    ): List<ApplicationRead>

    // 탈퇴 cascade로 soft delete된 기록은 존재 확인에 보이지 않지만 유니크 제약에는 걸려
    // 확인 후 저장으로는 재가입한 매니저가 예전에 읽은 지원서를 열 때마다 충돌함
    @Modifying
    @Query(
        """
        INSERT INTO application_reads (application_id, user_id, created_at, updated_at)
        VALUES (:applicationId, :userId, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE deleted_at = NULL, updated_at = UTC_TIMESTAMP(6)
        """,
        nativeQuery = true,
    )
    fun insertOrRestore(
        applicationId: Long,
        userId: UUID,
    )

    @Modifying
    @Query(
        """
        UPDATE application_reads SET deleted_at = UTC_TIMESTAMP(6)
        WHERE application_id = :applicationId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationIdAndDeletedAtIsNull(applicationId: Long)

    @Modifying
    @Query(
        """
        UPDATE application_reads SET deleted_at = UTC_TIMESTAMP(6)
        WHERE user_id = :userId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.post_id = :postId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationPostIdAndDeletedAtIsNull(postId: Long)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.team_id = :teamId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationTeamIdAndDeletedAtIsNull(teamId: Long)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.user_id = :userId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        JOIN posts p ON p.id = a.post_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE p.user_id = :userId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationPostUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.user_id = :userId
        AND a.team_id = :teamId
        AND a.id <> :excludedId
        AND a.status = 'PENDING'
        AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationUserIdAndTeamIdAndApplicationIdNotAndStatusPendingAndDeletedAtIsNull(
        userId: UUID,
        teamId: Long,
        excludedId: Long,
    )
}
