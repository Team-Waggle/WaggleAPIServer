package io.waggle.waggleapiserver.domain.recruitment.repository

import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import org.springframework.data.jpa.repository.JpaRepository

interface RecruitmentRepository : JpaRepository<Recruitment, Long> {
    fun findByTeamIdAndPosition(
        teamId: Long,
        position: Position,
    ): Recruitment?

    fun findByTeamId(teamId: Long): List<Recruitment>
}
