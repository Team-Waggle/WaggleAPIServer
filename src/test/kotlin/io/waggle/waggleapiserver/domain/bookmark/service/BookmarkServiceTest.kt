package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.request.BookmarkToggleRequest
import io.waggle.waggleapiserver.domain.post.dto.response.BookmarkedPostResponse
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BookmarkServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var bookmarkService: BookmarkService

    @Test
    fun `모집글 북마크를 켜면 행이 생기고 다시 누르면 지워진다`() {
        val author = createUser("author")
        val user = createUser("user")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val request = BookmarkToggleRequest(targetId = post.id, type = BookmarkType.POST)

        val on = bookmarkService.toggleBookmark(request, user)

        assertThat(on.bookmarked).isTrue()
        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE type = 'POST' AND target_id = ?", post.id))
            .isEqualTo(1L)

        val off = bookmarkService.toggleBookmark(request, user)

        assertThat(off.bookmarked).isFalse()
        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE type = 'POST' AND target_id = ?", post.id))
            .isZero()
    }

    @Test
    fun `북마크한 모집글과 팀이 내 북마크 목록에 나온다`() {
        val author = createUser("author")
        val user = createUser("user")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        bookmarkService.toggleBookmark(BookmarkToggleRequest(targetId = post.id, type = BookmarkType.POST), user)
        bookmarkService.toggleBookmark(BookmarkToggleRequest(targetId = team.id, type = BookmarkType.TEAM), user)

        val posts = bookmarkService.getUserBookmarkables(BookmarkType.POST, user)
        val teams = bookmarkService.getUserBookmarkables(BookmarkType.TEAM, user)

        assertThat(posts.map { (it as BookmarkedPostResponse).id }).containsExactly(post.id)
        assertThat(teams.map { (it as TeamResponse).id }).containsExactly(team.id)
    }

    @Test
    fun `없는 모집글이나 팀은 북마크할 수 없다`() {
        val user = createUser("user")

        assertThatThrownBy {
            bookmarkService.toggleBookmark(BookmarkToggleRequest(targetId = 999L, type = BookmarkType.POST), user)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
        assertThatThrownBy {
            bookmarkService.toggleBookmark(BookmarkToggleRequest(targetId = 999L, type = BookmarkType.TEAM), user)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
        assertThat(count("SELECT COUNT(*) FROM bookmarks")).isZero()
    }
}
