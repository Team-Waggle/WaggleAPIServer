package io.waggle.waggleapiserver.common

import io.waggle.waggleapiserver.domain.follow.service.FollowService
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

/**
 * soft delete된 행을 되살리는 경로가 @SQLRestriction 아래에서도 동작하는지 확인
 * 복원은 native로 행을 찾은 뒤 deletedAt을 null로 되돌리는 구조라,
 * restriction이 그 경로를 막으면 재가입과 재팔로우가 조용히 깨짐
 */
class ReactivationRegressionTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var followService: FollowService

    @Test
    fun `탈퇴 cascade로 지워진 팔로우를 다시 누르면 되살아난다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")

        followService.follow(followee.id, follower)
        // 언팔로우는 hard delete라 이 경로가 아님
        // 탈퇴 cascade가 만드는 상태를 재현
        jdbcTemplate.update(
            "UPDATE follows SET deleted_at = UTC_TIMESTAMP(6) WHERE follower_id = UUID_TO_BIN(?)",
            follower.id.toString(),
        )

        assertThat(followService.follow(followee.id, follower)).isTrue()
        assertThat(
            count(
                "SELECT COUNT(*) FROM follows WHERE follower_id = UUID_TO_BIN(?) AND deleted_at IS NULL",
                follower.id.toString(),
            ),
        ).isEqualTo(1L)
    }

    @Test
    @Transactional
    fun `탈퇴한 사용자를 되살리면 다시 조회된다`() {
        val user = createUser("rejoiner")
        userService.deactivateUser(user)

        // CustomOAuth2UserService와 같은 순서 — 같은 트랜잭션 안에서 native 조회 후 복원
        val deleted = userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull("google", "rejoiner")
        assertThat(deleted).isNotNull()
        deleted!!.reactivate()
        followRepository.updateDeletedAtNullByUserIdAndPartnerActive(deleted.id)
        userRepository.save(deleted)

        assertThat(userRepository.findById(user.id).orElse(null)).isNotNull()
    }
}
