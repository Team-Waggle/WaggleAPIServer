package io.waggle.waggleapiserver.domain.recruitment.repository

import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RecruitmentRepository : JpaRepository<Recruitment, Long> {
    fun findByPostIdAndPosition(
        postId: Long,
        position: Position,
    ): Recruitment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByPostIdAndPosition(
        postId: Long,
        position: Position,
    ): Recruitment?

    fun findByPostId(postId: Long): List<Recruitment>

    fun findByPostIdIn(postIds: List<Long>): List<Recruitment>

    // 파생 판정이 이미 지원을 막고 있고, 이 쿼리는 응답의 포지션별 status를 화면과 일치시키는 역할
    @Modifying
    @Query(
        """
        UPDATE recruitments r
        JOIN posts p ON p.id = r.post_id
        SET r.status = 'CLOSED', r.updated_at = UTC_TIMESTAMP(6)
        WHERE p.deleted_at IS NULL
        AND p.expires_at IS NOT NULL
        AND p.expires_at <= UTC_TIMESTAMP(6)
        AND r.status <> 'CLOSED'
        """,
        nativeQuery = true,
    )
    fun updateStatusToClosedByPostExpired(): Int

    @Modifying
    @Query(
        """
        DELETE FROM recruitments
        WHERE post_id IN (SELECT id FROM posts WHERE team_id = :teamId)
        """,
        nativeQuery = true,
    )
    fun deleteByPostTeamId(teamId: Long)

    fun deleteByPostId(postId: Long)
}
