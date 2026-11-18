package io.waggle.waggleapiserver.domain.term.repository

import io.waggle.waggleapiserver.domain.term.UserTermAgreement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserTermAgreementRepository : JpaRepository<UserTermAgreement, Long> {
    @Query(
        """
        SELECT COUNT(uta) FROM UserTermAgreement uta
        WHERE uta.userId = :userId
        AND uta.termId IN :termIds
        AND uta.withdrawnAt IS NULL
        """,
    )
    fun countActiveByUserIdAndTermIdIn(
        userId: UUID,
        termIds: List<Long>,
    ): Long

    @Query(
        """
        SELECT uta.termId FROM UserTermAgreement uta
        WHERE uta.userId = :userId AND uta.withdrawnAt IS NULL
        """,
    )
    fun findActiveTermIdsByUserId(userId: UUID): List<Long>

    // 중복 제출이 확인 후 저장을 함께 통과하면 유니크 충돌이 나므로 INSERT 하나로 처리함
    @Modifying
    @Query(
        """
        INSERT IGNORE INTO user_term_agreements (user_id, term_id, agreed_at)
        VALUES (:userId, :termId, UTC_TIMESTAMP(6))
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        userId: UUID,
        termId: Long,
    )

    fun deleteByUserId(userId: UUID)
}
