package io.waggle.waggleapiserver.domain.follow.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.follow.dto.request.FollowToggleRequest
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

class FollowServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var followService: FollowService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `재가입해도 아직 탈퇴 상태인 상대와의 팔로우는 되살아나지 않는다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        followService.toggleFollow(FollowToggleRequest(followee.id), follower)
        userService.deactivateUser(follower)
        userService.deactivateUser(followee)

        rejoin("followee")

        assertThat(
            count(
                "SELECT COUNT(*) FROM follows WHERE followee_id = UUID_TO_BIN(?) AND deleted_at IS NULL",
                followee.id.toString(),
            ),
        ).isZero()
        assertThat(followService.getUserFollowers(followee.id)).isEmpty()
    }

    @Test
    fun `상대가 먼저 재가입해 있으면 나중에 재가입할 때 팔로우가 되살아난다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        followService.toggleFollow(FollowToggleRequest(followee.id), follower)
        userService.deactivateUser(follower)
        userService.deactivateUser(followee)

        rejoin("follower")
        rejoin("followee")

        assertThat(
            count(
                "SELECT COUNT(*) FROM follows WHERE follower_id = UUID_TO_BIN(?) AND deleted_at IS NULL",
                follower.id.toString(),
            ),
        ).isEqualTo(1L)
    }

    @Test
    fun `프로필을 설정하지 않은 재가입자는 상대의 팔로잉 목록과 수에서 빠진다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        followService.toggleFollow(FollowToggleRequest(followee.id), follower)
        userService.deactivateUser(followee)

        rejoin("followee")

        assertThat(followService.getUserFollowees(follower.id)).isEmpty()
        assertThat(followService.getUserFollowCounts(follower.id).following).isZero()
    }

    @Test
    fun `프로필을 설정하지 않은 사용자는 팔로우할 수 없다`() {
        val follower = createUser("follower")
        val newcomer =
            userRepository.save(
                User(
                    provider = "google",
                    providerId = "newcomer",
                    email = "newcomer@test.com",
                    profileImageUrl = null,
                ),
            )

        assertThatThrownBy { followService.toggleFollow(FollowToggleRequest(newcomer.id), follower) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `프로필을 설정하지 않은 재가입자를 언팔로우할 수 있다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        followService.toggleFollow(FollowToggleRequest(followee.id), follower)
        userService.deactivateUser(followee)
        rejoin("followee")

        val result = followService.toggleFollow(FollowToggleRequest(followee.id), follower)

        assertThat(result.following).isFalse()
    }

    // CustomOAuth2UserService의 재가입 분기와 같은 순서
    private fun rejoin(providerId: String) {
        transactionTemplate.executeWithoutResult {
            val deleted = userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull("google", providerId)!!
            deleted.reactivate()
            followRepository.updateDeletedAtNullByUserIdAndPartnerActive(deleted.id)
        }
    }
}
