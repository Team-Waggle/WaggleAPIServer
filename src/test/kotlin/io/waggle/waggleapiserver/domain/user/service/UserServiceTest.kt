package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
}
