package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.dto.request.MemberUpdateRoleRequest
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
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
    fun updateMemberRole(
        memberId: Long,
        request: MemberUpdateRoleRequest,
        user: User,
    ): MemberResponse {
        val role = request.role

        val targetMember =
            memberRepository.findByIdOrNull(memberId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        if (user.id == targetMember.userId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Cannot update your own role")
        }

        val project =
            projectRepository.findByIdOrNull(targetMember.projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Project Not Found: ${targetMember.projectId}")

        val member =
            memberRepository.findByUserIdAndProjectId(user.id, project.id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")

        when (role) {
            MemberRole.MEMBER, MemberRole.MANAGER -> member.checkMemberRole(MemberRole.LEADER)
            MemberRole.LEADER -> delegateLeader(targetMember, member)
        }

        targetMember.updateRole(role)

        return MemberResponse.of(targetMember, user)
    }

    @Transactional
    fun leaveProject(
        projectId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member Not Found")
        val members =
            memberRepository.findByIdNotAndProjectIdOrderByCreatedAtAsc(member.id, projectId)
        if (members.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Cannot leave as the only member")
        }

        if (member.isLeader) {
            delegateLeader(members[0], member)
        }

        member.delete()
    }

    @Transactional
    fun removeMember(
        memberId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByIdOrNull(memberId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found: $memberId")

        val project =
            projectRepository.findByIdOrNull(member.projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Project Not Found: ${member.projectId}")

        val leader =
            memberRepository.findByUserIdAndProjectId(user.id, project.id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        if (!leader.isLeader) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Only leader can remove member")
        }

        member.delete()
    }

    private fun delegateLeader(
        member: Member,
        leader: Member,
    ) {
        if (!leader.isLeader) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Only leader can delegate authority")
        }
        if (member.projectId != leader.projectId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Not in the same project")
        }

        val project =
            projectRepository.findByIdOrNull(leader.projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Project Not Found: ${leader.projectId}")

        leader.updateRole(MemberRole.MANAGER)
        member.updateRole(MemberRole.LEADER)
        project.leaderId = member.userId
    }
}
