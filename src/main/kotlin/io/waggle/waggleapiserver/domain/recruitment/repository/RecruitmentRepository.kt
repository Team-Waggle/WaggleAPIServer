package io.waggle.waggleapiserver.domain.recruitment.repository

import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

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
}
