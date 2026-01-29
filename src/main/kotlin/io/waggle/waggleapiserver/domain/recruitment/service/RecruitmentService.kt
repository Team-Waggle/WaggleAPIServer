package io.waggle.waggleapiserver.domain.recruitment.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RecruitmentService(
    private val memberRepository: MemberRepository,
    private val recruitmentRepository: RecruitmentRepository,
) {
    @Transactional
    fun createRecruitments(
        projectId: Long,
        request: List<RecruitmentUpsertRequest>,
        user: User,
    ): List<RecruitmentResponse> {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found: ${user.id}, $projectId")
        member.checkMemberRole(MemberRole.MANAGER)

        val existingPositions =
            recruitmentRepository
                .findByProjectId(projectId)
                .map { it.position }
        val duplicates = request.map { it.position }.intersect(existingPositions.toSet())
        if (duplicates.isNotEmpty()) {
            throw BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Already exists recruitment for positions: $duplicates")
        }

        val recruitments =
            request.map {
                Recruitment(
                    position = it.position,
                    recruitingCount = it.recruitingCount,
                    projectId = projectId,
                )
            }
        val savedRecruitments = recruitmentRepository.saveAll(recruitments)

        return savedRecruitments.map { RecruitmentResponse.from(it) }
    }
}
