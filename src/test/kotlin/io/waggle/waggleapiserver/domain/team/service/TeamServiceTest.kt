package io.waggle.waggleapiserver.domain.team.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.ImageDirectory
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.team.dto.request.TeamUpsertRequest
import io.waggle.waggleapiserver.domain.team.enums.WorkMode
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.repository.findByIdOrNull

class TeamServiceTest : CascadeIntegrationTestSupport() {
    // storageClient는 목이라 isUploadedBy는 스텁한 경우에만 true
    @Test
    fun `남이 올린 이미지 URL로는 팀을 만들거나 팀 이미지를 바꿀 수 없다`() {
        val manager = createUser("manager")
        val team = createTeam(manager.id)
        createMember(manager.id, team.id, MemberRole.LEADER)

        assertRejected { teamService.createTeam(teamUpsertRequest(team.name, VICTIM_URL), manager) }
        assertRejected { teamService.updateTeam(team.id, teamUpsertRequest(team.name, VICTIM_URL), manager) }

        assertThat(teamRepository.findByIdOrNull(team.id)?.profileImageUrl).isNull()
        verify(storageClient, never()).delete(anyString())
    }

    @Test
    fun `팀 이미지는 수정하는 매니저가 직접 올린 것이면 바꿀 수 있다`() {
        val leader = createUser("leader")
        val manager = createUser("manager")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(manager.id, team.id, MemberRole.MANAGER)
        val uploaded = "https://bucket/prod/teams/${manager.id}/22222222-2222-2222-2222-222222222222.png"
        given(storageClient.isUploadedBy(uploaded, ImageDirectory.TEAM_PROFILE, manager.id)).willReturn(true)

        teamService.updateTeam(team.id, teamUpsertRequest(team.name, uploaded), manager)

        assertThat(teamRepository.findByIdOrNull(team.id)?.profileImageUrl).isEqualTo(uploaded)
    }

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
    }
}
