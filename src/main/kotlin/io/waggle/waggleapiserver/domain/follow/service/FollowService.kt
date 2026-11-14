package io.waggle.waggleapiserver.domain.follow.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountsResponse
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
    fun follow(
        followeeId: UUID,
        user: User,
    ): Boolean {
        if (followeeId == user.id) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Cannot follow yourself")
        }
        checkFollowable(followeeId)

        if (followRepository.insertIfAbsent(user.id, followeeId) == 1) {
            return true
        }
        // 무시된 INSERT IGNORE는 기존 행에 공유 잠금을 남기므로, 동시 요청이 모두 UPDATE로 가면 서로의 잠금을 기다려 데드락이 남
        // 잠금 없는 조회로 되살릴 행이 있을 때만 UPDATE함
        // 탈퇴 cascade로 soft delete된 행은 INSERT를 무시시키므로 되살려야 팔로우가 됨
        if (followRepository.countByFollowerIdAndFolloweeIdAndDeletedAtIsNotNull(user.id, followeeId) == 0L) {
            return false
        }
        return followRepository.updateDeletedAtNullByFollowerIdAndFolloweeIdAndDeletedAtIsNotNull(
            user.id,
            followeeId,
        ) == 1
    }

    @Transactional
    fun unfollow(
        followeeId: UUID,
        user: User,
    ) {
        if (!userRepository.existsById(followeeId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $followeeId")
        }
        followRepository.deleteByFollowerIdAndFolloweeId(user.id, followeeId)
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
    // 언팔로우에는 걸지 않음
    private fun checkFollowable(followeeId: UUID) {
        if (!userRepository.existsByIdAndUsernameIsNotNullAndPositionIsNotNull(followeeId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $followeeId")
        }
    }
}
