package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserCheckUsernameResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileCompletionResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun setupProfile(
        request: UserSetupProfileRequest,
        user: User,
    ): UserDetailResponse {
        if (user.username != null) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Profile already set up")
        }

        val (username, position, bio, skills, portfolioUrls) = request

        if (userRepository.existsByUsername(username)) {
            throw BusinessException(ErrorCode.DUPLICATE_RESOURCE, "$username exists already")
        }

        user.setupProfile(
            username = username,
            position = position,
            bio = bio,
            skills = skills,
            portfolioUrls = portfolioUrls,
        )

        return UserDetailResponse.from(user)
    }

    fun checkUsername(username: String): UserCheckUsernameResponse {
        val isAvailable = !userRepository.existsByUsername(username)
        return UserCheckUsernameResponse(isAvailable)
    }

    fun getUser(userId: UUID): UserDetailResponse {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $userId")
        return UserDetailResponse.from(user)
    }

    fun getUserProjects(userId: UUID): List<ProjectSimpleResponse> {
        val projectIds =
            memberRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .map { it.projectId }
        val projects = projectRepository.findAllById(projectIds)

        return projects.map { ProjectSimpleResponse.from(it) }
    }

    fun getUserProfileCompletion(user: User): UserProfileCompletionResponse = UserProfileCompletionResponse(user.isProfileComplete())

    @Transactional
    fun updateUser(
        request: UserUpdateRequest,
        user: User,
    ): UserDetailResponse {
        val (position, bio, skills, portfolioUrls) = request

        user.update(
            bio = bio,
            position = position,
            skills = skills,
            portfolioUrls = portfolioUrls,
        )

        return UserDetailResponse.from(user)
    }
}
