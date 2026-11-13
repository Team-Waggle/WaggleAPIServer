package io.waggle.waggleapiserver.domain.follow.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.follow.Follow
import io.waggle.waggleapiserver.domain.follow.dto.request.FollowToggleRequest
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountsResponse
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowToggleResponse
import io.waggle.waggleapiserver.domain.follow.repository.FollowRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun toggleFollow(
        request: FollowToggleRequest,
        user: User,
    ): FollowToggleResponse {
        val followeeId = request.userId
        val followerId = user.id

        if (followeeId == user.id) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Cannot follow yourself")
        }

        if (!userRepository.existsById(followeeId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $followeeId")
        }

        val existingFollow =
            followRepository.findByFollowerIdAndFolloweeIdIncludingDeleted(followerId, followeeId)
        return when {
            existingFollow == null -> {
                checkFollowable(followeeId)
                followRepository.save(
                    Follow(
                        followerId = followerId,
                        followeeId = followeeId,
                    ),
                )
                FollowToggleResponse(following = true)
            }

            existingFollow.deletedAt == null -> {
                followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId)
                FollowToggleResponse(following = false)
            }

            else -> {
                checkFollowable(followeeId)
                existingFollow.reactivate()
                FollowToggleResponse(following = true)
            }
        }
    }

    fun getUserFollowees(userId: UUID): List<UserSimpleResponse> =
        followRepository.findFolloweesWithCompleteProfile(userId).map { UserSimpleResponse.from(it) }

    fun getUserFollowers(userId: UUID): List<UserSimpleResponse> =
        followRepository.findFollowersWithCompleteProfile(userId).map { UserSimpleResponse.from(it) }

    fun getUserFollowCounts(userId: UUID): FollowCountsResponse =
        FollowCountsResponse(
            followed = followRepository.countFollowersWithCompleteProfile(userId).toInt(),
            following = followRepository.countFolloweesWithCompleteProfile(userId).toInt(),
        )

    // 프로필이 없는 사용자를 팔로우하면 상대의 팔로잉 목록에서 응답을 만들 수 없음
    // 언팔로우는 막지 않음
    private fun checkFollowable(followeeId: UUID) {
        if (!userRepository.existsByIdAndUsernameIsNotNullAndPositionIsNotNull(followeeId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $followeeId")
        }
    }
}
