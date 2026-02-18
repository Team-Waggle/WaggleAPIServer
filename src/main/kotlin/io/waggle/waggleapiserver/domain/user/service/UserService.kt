package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.ImageDeleteEvent
import io.waggle.waggleapiserver.common.storage.StorageClient
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamSimpleResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserCheckUsernameResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileCompletionResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
    private val eventPublisher: ApplicationEventPublisher,
    private val storageClient: StorageClient,
    private val memberRepository: MemberRepository,
    private val teamRepository: TeamRepository,
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

    fun generateProfileImagePresignedUrl(
        request: PresignedUrlRequest,
        user: User,
    ): PresignedUrlResponse {
        val presignedUploadUrl =
            storageClient.generateUploadUrl("users", request.contentType)
        return PresignedUrlResponse.from(presignedUploadUrl)
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

    fun getUserTeams(userId: UUID): List<TeamSimpleResponse> {
        val teamIds =
            memberRepository
                .findByUserIdOrderByRoleAscCreatedAtAsc(userId)
                .map { it.teamId }
        val teamById = teamRepository.findAllById(teamIds).associateBy { it.id }

        return teamIds.mapNotNull { teamId ->
            teamById[teamId]?.let { TeamSimpleResponse.from(it) }
        }
    }

    fun getUserProfileCompletion(user: User): UserProfileCompletionResponse = UserProfileCompletionResponse(user.isProfileComplete())

    @Transactional
    fun updateUser(
        request: UserUpdateRequest,
        user: User,
    ): UserDetailResponse {
        val (position, bio, profileImageUrl, skills, portfolioUrls) = request

        user.profileImageUrl?.takeIf { it != profileImageUrl }?.let {
            eventPublisher.publishEvent(ImageDeleteEvent(it))
        }

        user.update(
            position = position,
            bio = bio,
            profileImageUrl = profileImageUrl,
            skills = skills,
            portfolioUrls = portfolioUrls,
        )

        return UserDetailResponse.from(user)
    }
}
