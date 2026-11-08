package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.ImageDirectory
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.springframework.data.repository.findByIdOrNull

class UserServiceTest : CascadeIntegrationTestSupport() {
    @Test
    fun `프로필 수정은 요청 중에 바뀐 협업 온도를 되돌리지 않는다`() {
        val staleUser = createUser("user")
        changeTemperatureElsewhere(staleUser, 37.0)

        userService.updateUser(
            UserUpdateRequest(position = Position.FRONTEND, bio = "new bio", skills = setOf(Skill.AWS)),
            staleUser,
        )

        assertThat(temperatureOf(staleUser)).isEqualTo(37.0)
        assertThat(
            jdbcTemplate.queryForObject("SELECT bio FROM users WHERE email = ?", String::class.java, staleUser.email),
        ).isEqualTo("new bio")
        assertThat(skillCountOf(staleUser)).isEqualTo(1L)
    }

    @Test
    fun `프로필 초기 설정은 요청 중에 바뀐 협업 온도를 되돌리지 않는다`() {
        val staleUser = userRepository.save(createUser("user").apply { username = null })
        changeTemperatureElsewhere(staleUser, 37.0)

        userService.setupProfile(
            UserSetupProfileRequest(username = "user", position = Position.BACKEND, skills = setOf(Skill.AWS)),
            staleUser,
        )

        assertThat(temperatureOf(staleUser)).isEqualTo(37.0)
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE email = ?",
                String::class.java,
                staleUser.email,
            ),
        ).isEqualTo("user")
        assertThat(skillCountOf(staleUser)).isEqualTo(1L)
    }

    @Test
    fun `탈퇴는 요청 중에 바뀐 협업 온도를 되돌리지 않는다`() {
        val staleUser = createUser("user")
        changeTemperatureElsewhere(staleUser, 37.0)

        userService.deactivateUser(staleUser)

        assertThat(temperatureOf(staleUser)).isEqualTo(37.0)
        assertThat(count("SELECT COUNT(*) FROM users WHERE email = ? AND deleted_at IS NOT NULL", staleUser.email))
            .isEqualTo(1L)
    }

    // storageClient는 목이라 isUploadedBy는 스텁한 경우에만 true
    @Test
    fun `남이 올린 이미지 URL로는 프로필 이미지를 바꿀 수 없다`() {
        val attacker = createUser("attacker", profileImageUrl = LEGACY_URL)

        assertRejected { userService.updateUser(userUpdateRequest(VICTIM_URL), attacker) }

        assertThat(userRepository.findByIdOrNull(attacker.id)?.profileImageUrl).isEqualTo(LEGACY_URL)
        verify(storageClient, never()).delete(anyString())
    }

    @Test
    fun `프로필 초기 설정에서도 남이 올린 이미지 URL은 거부한다`() {
        val attacker = userRepository.save(createUser("attacker").apply { username = null })

        assertRejected {
            userService.setupProfile(
                UserSetupProfileRequest(
                    username = "attacker",
                    position = Position.BACKEND,
                    profileImageUrl = VICTIM_URL,
                    skills = emptySet(),
                ),
                attacker,
            )
        }
    }

    @Test
    fun `이미지를 그대로 두면 과거 형식이나 외부 URL도 유지되고 아무것도 지우지 않는다`() {
        val legacyUser = createUser("legacy", profileImageUrl = LEGACY_URL)
        val oauthUser = createUser("oauth", profileImageUrl = OAUTH_URL)

        userService.updateUser(userUpdateRequest(LEGACY_URL), legacyUser)
        userService.updateUser(userUpdateRequest(OAUTH_URL), oauthUser)

        assertThat(userRepository.findByIdOrNull(legacyUser.id)?.profileImageUrl).isEqualTo(LEGACY_URL)
        assertThat(userRepository.findByIdOrNull(oauthUser.id)?.profileImageUrl).isEqualTo(OAUTH_URL)
        verify(storageClient, never()).delete(anyString())
    }

    @Test
    fun `직접 올린 이미지로 바꾸면 저장되고 이전 이미지 삭제를 요청한다`() {
        val user = createUser("user", profileImageUrl = LEGACY_URL)
        val uploaded = "https://bucket/prod/users/${user.id}/11111111-1111-1111-1111-111111111111.png"
        given(storageClient.isUploadedBy(uploaded, ImageDirectory.USER_PROFILE, user.id)).willReturn(true)

        userService.updateUser(userUpdateRequest(uploaded), user)

        assertThat(userRepository.findByIdOrNull(user.id)?.profileImageUrl).isEqualTo(uploaded)
        verify(storageClient, timeout(5_000)).delete(LEGACY_URL)
    }

    // @CurrentUser로 받는 User는 요청 시작 때 읽힌 분리된 사본
    // 그 사이 다른 트랜잭션이 협업 온도를 바꾼 상황을 JDBC UPDATE로 재현함
    private fun changeTemperatureElsewhere(
        user: User,
        temperature: Double,
    ) {
        jdbcTemplate.update("UPDATE users SET temperature = ? WHERE email = ?", temperature, user.email)
    }

    private fun skillCountOf(user: User): Long =
        count("SELECT COUNT(*) FROM user_skills s JOIN users u ON u.id = s.user_id WHERE u.email = ?", user.email)

    private fun temperatureOf(user: User): Double =
        jdbcTemplate.queryForObject("SELECT temperature FROM users WHERE email = ?", Double::class.java, user.email)!!

    private fun createUser(
        name: String,
        profileImageUrl: String?,
    ): User = userRepository.save(createUser(name).apply { this.profileImageUrl = profileImageUrl })

    private fun userUpdateRequest(profileImageUrl: String?) =
        UserUpdateRequest(position = Position.BACKEND, profileImageUrl = profileImageUrl, skills = emptySet())

    private fun assertRejected(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
    }

    companion object {
        private const val VICTIM_URL =
            "https://bucket/prod/users/00000000-0000-0000-0000-00000000000a/33333333-3333-3333-3333-333333333333.png"
        private const val LEGACY_URL = "https://bucket/prod/users/44444444-4444-4444-4444-444444444444.png"
        private const val OAUTH_URL = "https://lh3.googleusercontent.com/a/profile-photo"
    }
}
