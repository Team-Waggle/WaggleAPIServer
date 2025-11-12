package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
) {
    @Transactional
    fun leaveProject(
        projectId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member Not Found")
        val members =
            memberRepository.findAllByIdNotAndProjectIdOrderByCreatedAtAsc(member.id, projectId)
        check(members.isNotEmpty()) { "Cannot leave as the only member" }

        if (member.isLeader) {
            delegateLeader(members[0].id, projectId, user)
        }

        member.delete()
    }

    @Transactional
    fun delegateLeader(
        memberId: Long,
        projectId: Long,
        user: User,
    ) {
        val leader =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member Not Found")
        require(!leader.isLeader) { "Only leader can delegate authority" }

        val member =
            memberRepository.findByIdOrNull(memberId)
                ?: throw EntityNotFoundException("Member not found: $memberId")
        require(member.projectId == projectId) { "Not a member the project" }

        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project Not Found: $projectId")

        leader.updateRole(MemberRole.MANAGER)
        member.updateRole(MemberRole.LEADER)
        project.leaderId = member.userId
    }
}
