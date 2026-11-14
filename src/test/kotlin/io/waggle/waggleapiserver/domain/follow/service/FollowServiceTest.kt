package io.waggle.waggleapiserver.domain.follow.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FollowServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var followService: FollowService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `재가입해도 아직 탈퇴 상태인 상대와의 팔로우는 되살아나지 않는다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        followService.follow(followee.id, follower)
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
        followService.follow(followee.id, follower)
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
        followService.follow(followee.id, follower)
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

        assertThatThrownBy { followService.follow(newcomer.id, follower) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `프로필을 설정하지 않은 재가입자를 언팔로우할 수 있다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        followService.follow(followee.id, follower)
        userService.deactivateUser(followee)
        rejoin("followee")

        followService.unfollow(followee.id, follower)

        assertThat(
            count(
                "SELECT COUNT(*) FROM follows WHERE follower_id = UUID_TO_BIN(?)",
                follower.id.toString(),
            ),
        ).isZero()
    }

    @Test
    fun `같은 사용자를 동시에 팔로우해도 전부 성공하고 한 건만 생성된다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        val threadCount = 8
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)

        val futures =
            (1..threadCount).map {
                executor.submit<Boolean> {
                    startLatch.await()
                    followService.follow(followee.id, follower)
                }
            }
        startLatch.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        assertThat(results.count { it }).isEqualTo(1)
        assertThat(
            count(
                "SELECT COUNT(*) FROM follows WHERE follower_id = UUID_TO_BIN(?)",
                follower.id.toString(),
            ),
        ).isEqualTo(1L)
    }

    @Test
    fun `언팔로우를 두 번 해도 없는 행 삭제는 무해하다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        followService.follow(followee.id, follower)

        followService.unfollow(followee.id, follower)
        followService.unfollow(followee.id, follower)

        assertThat(followService.getUserFollowCounts(follower.id).following).isZero()
    }

    @Test
    fun `자기 자신은 팔로우할 수 없다`() {
        val user = createUser("user")

        assertThatThrownBy { followService.follow(user.id, user) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
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
