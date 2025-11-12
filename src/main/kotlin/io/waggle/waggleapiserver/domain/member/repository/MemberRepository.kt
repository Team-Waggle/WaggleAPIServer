package io.waggle.waggleapiserver.domain.member.repository

import io.waggle.waggleapiserver.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByUserIdAndProjectId(
        userId: UUID,
        projectId: Long,
    ): Member?

    fun findAllByIdNotAndProjectIdOrderByCreatedAtAsc(
        id: Long,
        projectId: Long,
    ): List<Member>

    fun findAllByUserIdOrderByCreatedAtAsc(userId: UUID): List<Member>

    fun findAllByProjectIdOrderByCreatedAtAsc(projectId: Long): List<Member>
}
