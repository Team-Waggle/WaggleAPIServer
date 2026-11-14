package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.post.dto.response.BookmarkedPostResponse
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BookmarkServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var bookmarkService: BookmarkService

    @Test
    fun `모집글 북마크를 두 번 해도 한 번 한 것과 같다`() {
        val author = createUser("author")
        val user = createUser("user")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        val first = bookmarkService.bookmark(BookmarkType.POST, post.id, user)
        val second = bookmarkService.bookmark(BookmarkType.POST, post.id, user)

        assertThat(first).isTrue()
        assertThat(second).isFalse()
        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE type = 'POST' AND target_id = ?", post.id))
            .isEqualTo(1L)
    }

    @Test
    fun `같은 북마크를 동시에 해도 전부 성공하고 한 건만 생성된다`() {
        val author = createUser("author")
        val user = createUser("user")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val threadCount = 8
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)

        val futures =
            (1..threadCount).map {
                executor.submit<Boolean> {
                    startLatch.await()
                    bookmarkService.bookmark(BookmarkType.POST, post.id, user)
                }
            }
        startLatch.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        assertThat(results.count { it }).isEqualTo(1)
        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE type = 'POST' AND target_id = ?", post.id))
            .isEqualTo(1L)
    }

    @Test
    fun `북마크 취소를 두 번 해도 없는 행 삭제는 무해하다`() {
        val author = createUser("author")
        val user = createUser("user")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        bookmarkService.bookmark(BookmarkType.POST, post.id, user)

        bookmarkService.unbookmark(BookmarkType.POST, post.id, user)
        bookmarkService.unbookmark(BookmarkType.POST, post.id, user)

        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE type = 'POST' AND target_id = ?", post.id))
            .isZero()
    }

    @Test
    fun `북마크한 모집글과 팀이 내 북마크 목록에 나온다`() {
        val author = createUser("author")
        val user = createUser("user")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        bookmarkService.bookmark(BookmarkType.POST, post.id, user)
        bookmarkService.bookmark(BookmarkType.TEAM, team.id, user)

        val posts = bookmarkService.getUserBookmarkables(BookmarkType.POST, user)
        val teams = bookmarkService.getUserBookmarkables(BookmarkType.TEAM, user)

        assertThat(posts.map { (it as BookmarkedPostResponse).id }).containsExactly(post.id)
        assertThat(teams.map { (it as TeamResponse).id }).containsExactly(team.id)
    }

    @Test
    fun `없는 모집글이나 팀은 북마크할 수 없다`() {
        val user = createUser("user")

        assertThatThrownBy { bookmarkService.bookmark(BookmarkType.POST, 999L, user) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
        assertThatThrownBy { bookmarkService.bookmark(BookmarkType.TEAM, 999L, user) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
        assertThat(count("SELECT COUNT(*) FROM bookmarks")).isZero()
    }
}
