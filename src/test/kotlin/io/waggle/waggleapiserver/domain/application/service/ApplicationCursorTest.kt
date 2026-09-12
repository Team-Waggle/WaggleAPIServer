package io.waggle.waggleapiserver.domain.application.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ApplicationCursorTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var applicationService: ApplicationService

    @Test
    fun `팀 지원 목록 커서는 상태 우선순위 경계를 건너뛰지도 겹치지도 않는다`() {
        val manager = createUser("manager")
        val team = createTeam(manager.id)
        createMember(manager.id, team.id, MemberRole.MANAGER)
        val post = createPost(manager.id, team.id)

        val first = createApplication(team.id, post.id, createUser("applicant-1").id)
        val second = createApplication(team.id, post.id, createUser("applicant-2").id)
        val third = createApplication(team.id, post.id, createUser("applicant-3").id)
        val fourth = createApplication(team.id, post.id, createUser("applicant-4").id)
        jdbcTemplate.update("UPDATE applications SET status = 'APPROVED' WHERE id IN (?, ?)", second.id, fourth.id)

        // PENDING(우선순위 0)이 id 내림차순으로 먼저, 그다음 처리된 것(우선순위 1)
        val firstPage = teamApplications(team.id, manager, size = 2)
        val secondPage = teamApplications(team.id, manager, size = 2, cursor = firstPage.nextCursor)

        assertThat(firstPage.data.map { it.id }).containsExactly(third.id, first.id)
        assertThat(firstPage.hasNext).isTrue()
        assertThat(secondPage.data.map { it.id }).containsExactly(fourth.id, second.id)
        assertThat(secondPage.hasNext).isFalse()
    }

    @Test
    fun `팀 지원 목록은 커서 지원서가 삭제돼도 다음 페이지가 이어진다`() {
        val manager = createUser("manager")
        val team = createTeam(manager.id)
        createMember(manager.id, team.id, MemberRole.MANAGER)
        val post = createPost(manager.id, team.id)

        val first = createApplication(team.id, post.id, createUser("applicant-1").id)
        val second = createApplication(team.id, post.id, createUser("applicant-2").id)
        val lastApplicant = createUser("applicant-3")
        val third = createApplication(team.id, post.id, lastApplicant.id)

        val firstPage = teamApplications(team.id, manager, size = 1)
        applicationService.deleteApplication(third.id, lastApplicant)
        val secondPage = teamApplications(team.id, manager, size = 1, cursor = firstPage.nextCursor)

        assertThat(firstPage.data.map { it.id }).containsExactly(third.id)
        assertThat(secondPage.data.map { it.id }).containsExactly(second.id)
        assertThat(secondPage.hasNext).isTrue()
        assertThat(first.id).isLessThan(second.id)
    }

    @Test
    fun `내 지원 목록 커서는 id 내림차순으로 이어진다`() {
        val leader = createUser("leader")
        val team = createTeam(leader.id)
        val applicant = createUser("applicant")
        val applications =
            (1..3).map { createApplication(team.id, createPost(leader.id, team.id).id, applicant.id) }

        val firstPage = applicationService.getUserApplications(null, CursorGetQuery(cursor = null, size = 2), applicant)
        val secondPage =
            applicationService.getUserApplications(
                null,
                CursorGetQuery(cursor = firstPage.nextCursor, size = 2),
                applicant,
            )

        assertThat(firstPage.data.map { it.id }).containsExactly(applications[2].id, applications[1].id)
        assertThat(secondPage.data.map { it.id }).containsExactly(applications[0].id)
        assertThat(secondPage.hasNext).isFalse()
    }

    private fun teamApplications(
        teamId: Long,
        manager: User,
        size: Int,
        cursor: String? = null,
    ) = applicationService.getTeamApplications(teamId, null, CursorGetQuery(cursor = cursor, size = size), manager)
}
