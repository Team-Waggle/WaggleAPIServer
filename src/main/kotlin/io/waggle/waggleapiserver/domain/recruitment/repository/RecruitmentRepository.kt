package io.waggle.waggleapiserver.domain.recruitment.repository

import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import org.springframework.data.jpa.repository.JpaRepository

interface RecruitmentRepository : JpaRepository<Recruitment, Long> {
    fun findByPostIdAndPosition(
        postId: Long,
        position: Position,
    ): Recruitment?

    fun findByPostId(postId: Long): List<Recruitment>

    fun findByPostIdIn(postIds: List<Long>): List<Recruitment>
}
