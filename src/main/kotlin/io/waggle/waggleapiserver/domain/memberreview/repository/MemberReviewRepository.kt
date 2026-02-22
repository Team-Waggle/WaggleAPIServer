package io.waggle.waggleapiserver.domain.memberreview.repository

import io.waggle.waggleapiserver.domain.memberreview.MemberReview
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemberReviewRepository : JpaRepository<MemberReview, Long> {
    fun findByReviewerIdAndRevieweeIdAndTeamId(
        reviewerId: UUID,
        revieweeId: UUID,
        teamId: Long,
    ): MemberReview?

    fun findByReviewerId(reviewerId: UUID): List<MemberReview>

    fun findByRevieweeId(revieweeId: UUID): List<MemberReview>
}
