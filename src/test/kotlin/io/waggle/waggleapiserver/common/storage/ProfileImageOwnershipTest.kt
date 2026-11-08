package io.waggle.waggleapiserver.common.storage

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.team.dto.request.TeamUpsertRequest
import io.waggle.waggleapiserver.domain.team.enums.WorkMode
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.enums.Position
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

// 이미지를 바꾸면 이전 이미지를 S3에서 지우므로, 남이 올린 객체의 URL이 저장되면 그 객체가 지워짐
// storageClient는 목이라 isUploadedBy는 스텁한 경우에만 true
class ProfileImageOwnershipTest : CascadeIntegrationTestSupport() {
    @Test
    fun `남이 올린 이미지 URL로는 프로필 이미지를 바꿀 수 없다`() {
        val attacker = createUser("attacker", profileImageUrl = LEGACY_URL)

        assertRejected { userService.updateUser(userUpdateRequest(VICTIM_URL), attacker) }

        assertThat(userRepository.findByIdOrNull(attacker.id)?.profileImageUrl).isEqualTo(LEGACY_URL)
        verify(storageClient, never()).delete(anyString())
    }

    @Test
    fun `프로필 초기 설정에서도 남이 올린 이미지 URL은 거부한다`() {
        val attacker = createUser("attacker", profileImageUrl = null).apply { username = null }
        userRepository.save(attacker)

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

    @Test
    fun `남이 올린 이미지 URL로는 팀을 만들거나 팀 이미지를 바꿀 수 없다`() {
        val manager = createUser("manager", profileImageUrl = null)
        val team = createTeam(manager.id)
        createMember(manager.id, team.id, MemberRole.LEADER)

        assertRejected { teamService.createTeam(teamUpsertRequest(team.name, VICTIM_URL), manager) }
        assertRejected { teamService.updateTeam(team.id, teamUpsertRequest(team.name, VICTIM_URL), manager) }

        assertThat(teamRepository.findByIdOrNull(team.id)?.profileImageUrl).isNull()
        verify(storageClient, never()).delete(anyString())
    }

    @Test
    fun `팀 이미지는 수정하는 매니저가 직접 올린 것이면 바꿀 수 있다`() {
        val leader = createUser("leader", profileImageUrl = null)
        val manager = createUser("manager", profileImageUrl = null)
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(manager.id, team.id, MemberRole.MANAGER)
        val uploaded = "https://bucket/prod/teams/${manager.id}/22222222-2222-2222-2222-222222222222.png"
        given(storageClient.isUploadedBy(uploaded, ImageDirectory.TEAM_PROFILE, manager.id)).willReturn(true)

        teamService.updateTeam(team.id, teamUpsertRequest(team.name, uploaded), manager)

        assertThat(teamRepository.findByIdOrNull(team.id)?.profileImageUrl).isEqualTo(uploaded)
    }

    private fun createUser(
        name: String,
        profileImageUrl: String?,
    ): User = createUser(name).apply { this.profileImageUrl = profileImageUrl }.let { userRepository.save(it) }

    private fun userUpdateRequest(profileImageUrl: String?) =
        UserUpdateRequest(position = Position.BACKEND, profileImageUrl = profileImageUrl, skills = emptySet())

    private fun teamUpsertRequest(
        name: String,
        profileImageUrl: String?,
    ) = TeamUpsertRequest(
        name = name,
        description = "description",
        workMode = WorkMode.ONLINE,
        profileImageUrl = profileImageUrl,
    )

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
