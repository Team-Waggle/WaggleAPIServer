package io.waggle.waggleapiserver.domain.team.service

import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener

@Import(TeamDeleteRollbackTest.FailingCascadeConfig::class)
class TeamDeleteRollbackTest : CascadeIntegrationTestSupport() {
    @TestConfiguration
    class FailingCascadeConfig {
        // TeamDeletedEvent를 같은 트랜잭션에서 동기로 받아 실패시켜 롤백을 유도
        @EventListener
        fun failOnTeamDeleted(event: TeamDeletedEvent): Unit = throw IllegalStateException("boom")
    }

    @Test
    fun `cascade 리스너가 실패하면 team과 모든 자식이 롤백된다`() {
        val leader = createUser("leader")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        createRecruitment(post.id)

        assertThatThrownBy { teamService.deleteTeam(team.id, leader) }
            .isInstanceOf(RuntimeException::class.java)

        // 전체 롤백: 아무것도 변경되지 않음
        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ? AND deleted_at IS NULL", team.id)).isEqualTo(1L)
        assertThat(
            count("SELECT COUNT(*) FROM members WHERE team_id = ? AND deleted_at IS NULL", team.id),
        ).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM posts WHERE team_id = ? AND deleted_at IS NULL", team.id)).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM recruitments WHERE post_id = ?", post.id)).isEqualTo(1L)
    }
}
