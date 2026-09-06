package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostFilterTest : CascadeIntegrationTestSupport() {
    @Test
    fun `포지션 필터는 해당 포지션을 모집하는 글만 남긴다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val backendPost = createPost(author.id, team.id)
        val designerPost = createPost(author.id, team.id)
        saveRecruitment(backendPost.id, Position.BACKEND)
        saveRecruitment(designerPost.id, Position.DESIGNER)

        val response = getPosts(PostGetQuery(positions = setOf(Position.BACKEND)))

        assertThat(response.map { it.id }).containsExactly(backendPost.id)
    }

    @Test
    fun `포지션 필터는 여러 값을 OR로 묶는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val backendPost = createPost(author.id, team.id)
        val designerPost = createPost(author.id, team.id)
        val frontendPost = createPost(author.id, team.id)
        saveRecruitment(backendPost.id, Position.BACKEND)
        saveRecruitment(designerPost.id, Position.DESIGNER)
        saveRecruitment(frontendPost.id, Position.FRONTEND)

        val response = getPosts(PostGetQuery(positions = setOf(Position.BACKEND, Position.FRONTEND)))

        assertThat(response.map { it.id }).containsExactly(frontendPost.id, backendPost.id)
    }

    @Test
    fun `스킬 필터는 모집 스킬 중 하나라도 일치하면 남긴다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val kotlinPost = createPost(author.id, team.id)
        val figmaPost = createPost(author.id, team.id)
        saveRecruitment(kotlinPost.id, Position.BACKEND, setOf(Skill.KOTLIN, Skill.SPRING))
        saveRecruitment(figmaPost.id, Position.DESIGNER, setOf(Skill.FIGMA))

        val response = getPosts(PostGetQuery(skills = setOf(Skill.SPRING)))

        assertThat(response.map { it.id }).containsExactly(kotlinPost.id)
    }

    @Test
    fun `포지션과 스킬 필터는 AND로 결합된다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val matched = createPost(author.id, team.id)
        val positionOnly = createPost(author.id, team.id)
        val skillOnly = createPost(author.id, team.id)
        saveRecruitment(matched.id, Position.BACKEND, setOf(Skill.KOTLIN))
        saveRecruitment(positionOnly.id, Position.BACKEND, setOf(Skill.REACT))
        saveRecruitment(skillOnly.id, Position.FRONTEND, setOf(Skill.KOTLIN))

        val response =
            getPosts(
                PostGetQuery(positions = setOf(Position.BACKEND), skills = setOf(Skill.KOTLIN)),
            )

        assertThat(response.map { it.id }).containsExactly(matched.id)
    }

    @Test
    fun `빈 필터는 조건으로 취급하지 않는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val withRecruitment = createPost(author.id, team.id)
        val withoutRecruitment = createPost(author.id, team.id)
        saveRecruitment(withRecruitment.id, Position.BACKEND)

        val response = getPosts(PostGetQuery(positions = emptySet(), skills = emptySet()))

        assertThat(response.map { it.id })
            .containsExactly(withoutRecruitment.id, withRecruitment.id)
    }

    private fun saveRecruitment(
        postId: Long,
        position: Position,
        skills: Set<Skill> = emptySet(),
    ): Recruitment =
        recruitmentRepository.save(
            Recruitment(
                position = position,
                count = 1,
                postId = postId,
                skills = skills.toMutableSet(),
            ),
        )

    private fun getPosts(query: PostGetQuery) = postService.getPosts(query, CursorGetQuery(cursor = null), null).data
}
