package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostSearchEscapeTest : CascadeIntegrationTestSupport() {
    @Test
    fun `검색어의 와일드카드는 리터럴로 취급된다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val plain = createPost(author.id, team.id)
        val withWildcard = createPost(author.id, team.id)
        jdbcTemplate.update("UPDATE posts SET title = ? WHERE id = ?", "백엔드 구인", plain.id)
        jdbcTemplate.update("UPDATE posts SET title = ? WHERE id = ?", "할인 50% 이벤트", withWildcard.id)

        val response =
            postService.getPosts(
                PostGetQuery(q = "%"),
                CursorGetQuery(cursor = null),
                null,
            )

        assertThat(response.data.map { it.id }).containsExactly(withWildcard.id)
    }
}
