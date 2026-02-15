package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.dto.request.MemberUpdateRoleRequest
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val teamRepository: TeamRepository,
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

        val team =
            teamRepository.findByIdOrNull(targetMember.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team Not Found: ${targetMember.teamId}",
                )

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, team.id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")

        when (role) {
            MemberRole.MEMBER, MemberRole.MANAGER -> member.checkMemberRole(MemberRole.LEADER)
            MemberRole.LEADER -> delegateLeader(targetMember, member)
        }

        targetMember.updateRole(role)

        return MemberResponse.of(targetMember, user)
    }

    @Transactional
    fun leaveTeam(
        teamId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member Not Found")
        val members =
            memberRepository.findByIdNotAndTeamIdOrderByCreatedAtAsc(member.id, teamId)
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
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: $memberId",
                )

        val team =
            teamRepository.findByIdOrNull(member.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team Not Found: ${member.teamId}",
                )

        val leader =
            memberRepository.findByUserIdAndTeamId(user.id, team.id)
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
        if (member.teamId != leader.teamId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Not in the same team")
        }

        val team =
            teamRepository.findByIdOrNull(leader.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team Not Found: ${leader.teamId}",
                )

        leader.updateRole(MemberRole.MANAGER)
        member.updateRole(MemberRole.LEADER)
        team.leaderId = member.userId
    }
}
