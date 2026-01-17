package io.waggle.waggleapiserver.domain.application.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun applyProject(
        projectId: Long,
        request: ApplicationCreateRequest,
        user: User,
    ): ApplicationResponse {
        val detail = request.detail

        val position = user.position ?: throw BusinessException(ErrorCode.INVALID_STATE, "User must have position")
        val recruitment =
            recruitmentRepository.findByProjectIdAndPosition(projectId, position)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Recruitment not found: $projectId, $position")

        if (!recruitment.isRecruiting()) {
            throw BusinessException(ErrorCode.INVALID_STATE, "$position is no longer recruiting")
        }

        if (applicationRepository.existsByProjectIdAndUserIdAndPosition(
                projectId,
                user.id,
                position,
            )
        ) {
            throw BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Already applied to project: $projectId")
        }

        val application =
            Application(
                position = position,
                projectId = projectId,
                userId = user.id,
                detail = detail,
            )
        val savedApplication = applicationRepository.save(application)

        return ApplicationResponse.from(savedApplication)
    }

    fun getUserApplications(user: User): List<ApplicationResponse> {
        val applications = applicationRepository.findByUserId(user.id)
        return applications.map { ApplicationResponse.from(it) }
    }

    fun getProjectApplications(
        projectId: Long,
        user: User,
    ): List<ApplicationResponse> {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MEMBER)

        val applications = applicationRepository.findByProjectId(projectId)

        return applications.map { ApplicationResponse.from(it) }
    }

    @Transactional
    fun approveApplication(
        applicationId: Long,
        user: User,
    ): ApplicationResponse {
        val application =
            applicationRepository.findByIdOrNull(applicationId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Application not found: $applicationId")

        val member =
            memberRepository.findByUserIdAndProjectId(user.id, application.projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MANAGER)

        application.updateStatus(ApplicationStatus.APPROVED)

        return ApplicationResponse.from(application)
    }

    @Transactional
    fun rejectApplication(
        applicationId: Long,
        user: User,
    ): ApplicationResponse {
        val application =
            applicationRepository.findByIdOrNull(applicationId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Application not found: $applicationId")

        val member =
            memberRepository.findByUserIdAndProjectId(user.id, application.projectId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MANAGER)

        application.updateStatus(ApplicationStatus.REJECTED)

        return ApplicationResponse.from(application)
    }

    @Transactional
    fun deleteApplication(
        applicationId: Long,
        user: User,
    ) {
        val application =
            applicationRepository.findByIdAndUserId(applicationId, user.id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Application not found: $applicationId")
        application.delete()
    }
}
