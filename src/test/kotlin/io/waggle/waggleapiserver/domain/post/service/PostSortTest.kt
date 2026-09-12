package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.post.Deadline
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PostSortTest : CascadeIntegrationTestSupport() {
    @Test
    fun `좋아요순은 좋아요 수 내림차순이고 동률은 최신순으로 끊는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val firstLiker = createUser("first-liker")
        val secondLiker = createUser("second-liker")

        val noLike = createPost(author.id, team.id)
        val oneLike = createPost(author.id, team.id)
        val twoLikes = createPost(author.id, team.id)
        val tiedWithNoLike = createPost(author.id, team.id)

        createLike(firstLiker.id, LikeType.POST, oneLike.id)
        createLike(firstLiker.id, LikeType.POST, twoLikes.id)
        createLike(secondLiker.id, LikeType.POST, twoLikes.id)

        val response =
            postService.getPosts(
                PostGetQuery(sort = PostSort.MOST_LIKED),
                CursorGetQuery(cursor = null),
                null,
            )

        assertThat(response.data.map { it.id })
            .containsExactly(twoLikes.id, oneLike.id, tiedWithNoLike.id, noLike.id)
        assertThat(response.data.map { it.likeCount }).containsExactly(2, 1, 0, 0)
    }

    @Test
    fun `조회순은 스케줄러가 반영한 조회수 내림차순이다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val leastViewed = createPost(author.id, team.id)
        val mostViewed = createPost(author.id, team.id)
        val middle = createPost(author.id, team.id)

        jdbcTemplate.update("UPDATE posts SET view_count = ? WHERE id = ?", 3, leastViewed.id)
        jdbcTemplate.update("UPDATE posts SET view_count = ? WHERE id = ?", 30, mostViewed.id)
        jdbcTemplate.update("UPDATE posts SET view_count = ? WHERE id = ?", 12, middle.id)

        val response =
            postService.getPosts(
                PostGetQuery(sort = PostSort.MOST_VIEWED),
                CursorGetQuery(cursor = null),
                null,
            )

        assertThat(response.data.map { it.id })
            .containsExactly(mostViewed.id, middle.id, leastViewed.id)
    }

    @Test
    fun `좋아요순 커서는 동률 구간을 건너뛰지도 겹치지도 않는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val liker = createUser("liker")

        val liked = createPost(author.id, team.id)
        val olderTie = createPost(author.id, team.id)
        val newerTie = createPost(author.id, team.id)
        createLike(liker.id, LikeType.POST, liked.id)

        val firstPage =
            postService.getPosts(
                PostGetQuery(sort = PostSort.MOST_LIKED),
                CursorGetQuery(cursor = null, size = 2),
                null,
            )
        val secondPage =
            postService.getPosts(
                PostGetQuery(sort = PostSort.MOST_LIKED),
                CursorGetQuery(cursor = firstPage.nextCursor, size = 2),
                null,
            )

        assertThat(firstPage.data.map { it.id }).containsExactly(liked.id, newerTie.id)
        assertThat(firstPage.hasNext).isTrue()
        assertThat(secondPage.data.map { it.id }).containsExactly(olderTie.id)
        assertThat(secondPage.hasNext).isFalse()
    }

    @Test
    fun `조회순 커서도 동률 구간에서 이어진다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val viewed = createPost(author.id, team.id)
        val olderTie = createPost(author.id, team.id)
        val newerTie = createPost(author.id, team.id)
        jdbcTemplate.update("UPDATE posts SET view_count = ? WHERE id = ?", 7, viewed.id)

        val firstPage =
            postService.getPosts(
                PostGetQuery(sort = PostSort.MOST_VIEWED),
                CursorGetQuery(cursor = null, size = 2),
                null,
            )
        val secondPage =
            postService.getPosts(
                PostGetQuery(sort = PostSort.MOST_VIEWED),
                CursorGetQuery(cursor = firstPage.nextCursor, size = 2),
                null,
            )

        assertThat(firstPage.data.map { it.id }).containsExactly(viewed.id, newerTie.id)
        assertThat(secondPage.data.map { it.id }).containsExactly(olderTie.id)
    }

    @Test
    fun `좋아요순도 검색어 필터를 함께 적용한다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val liker = createUser("liker")

        val matched = createPost(author.id, team.id)
        val unmatched = createPost(author.id, team.id)
        jdbcTemplate.update("UPDATE posts SET title = ? WHERE id = ?", "백엔드 구인", matched.id)
        jdbcTemplate.update("UPDATE posts SET title = ? WHERE id = ?", "디자이너 구인", unmatched.id)
        createLike(liker.id, LikeType.POST, unmatched.id)

        val response =
            postService.getPosts(
                PostGetQuery(q = "백엔드", sort = PostSort.MOST_LIKED),
                CursorGetQuery(cursor = null),
                null,
            )

        assertThat(response.data.map { it.id }).containsExactly(matched.id)
    }

    @Test
    fun `마감 임박순은 모집 중 기한 있음 - 모집 중 무기한 - 마감 순서로 이어진다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val soon = openPost(author.id, team.id, LocalDate.now().plusDays(1))
        val later = openPost(author.id, team.id, LocalDate.now().plusDays(7))
        val indefinite = openPost(author.id, team.id, null)
        val expired = openPost(author.id, team.id, LocalDate.now().minusDays(1))
        val closed = closedPost(author.id, team.id, LocalDate.now().plusDays(3))

        val response = deadlineSoon(size = 20)

        assertThat(response.data.map { it.id })
            .containsExactly(soon.id, later.id, indefinite.id, closed.id, expired.id)
    }

    @Test
    fun `마감 임박순의 마감 묶음은 만료일이 아니라 최신순이다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val expiredLongAgo = openPost(author.id, team.id, LocalDate.now().minusDays(10))
        val expiredRecently = openPost(author.id, team.id, LocalDate.now().minusDays(1))

        val response = deadlineSoon(size = 20)

        assertThat(response.data.map { it.id })
            .containsExactly(expiredRecently.id, expiredLongAgo.id)
    }

    @Test
    fun `마감 임박순 커서는 묶음 경계를 건너뛰지도 겹치지도 않는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val soon = openPost(author.id, team.id, LocalDate.now().plusDays(1))
        val later = openPost(author.id, team.id, LocalDate.now().plusDays(7))
        val indefinite = openPost(author.id, team.id, null)
        val expired = openPost(author.id, team.id, LocalDate.now().minusDays(1))

        val firstPage = deadlineSoon(size = 2)
        val secondPage = deadlineSoon(size = 2, cursor = firstPage.nextCursor)

        assertThat(firstPage.data.map { it.id }).containsExactly(soon.id, later.id)
        assertThat(firstPage.hasNext).isTrue()
        assertThat(secondPage.data.map { it.id }).containsExactly(indefinite.id, expired.id)
        assertThat(secondPage.hasNext).isFalse()
    }

    @Test
    fun `최신순은 id 내림차순이고 커서로 이어진다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val oldest = createPost(author.id, team.id)
        val middle = createPost(author.id, team.id)
        val newest = createPost(author.id, team.id)

        val firstPage = list(PostSort.NEWEST, size = 2)
        val secondPage = list(PostSort.NEWEST, size = 2, cursor = firstPage.nextCursor)

        assertThat(firstPage.data.map { it.id }).containsExactly(newest.id, middle.id)
        assertThat(secondPage.data.map { it.id }).containsExactly(oldest.id)
        assertThat(secondPage.hasNext).isFalse()
    }

    @Test
    fun `오래된 순은 id 오름차순이고 커서로 이어진다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val oldest = createPost(author.id, team.id)
        val middle = createPost(author.id, team.id)
        val newest = createPost(author.id, team.id)

        val firstPage = list(PostSort.OLDEST, size = 2)
        val secondPage = list(PostSort.OLDEST, size = 2, cursor = firstPage.nextCursor)

        assertThat(firstPage.data.map { it.id }).containsExactly(oldest.id, middle.id)
        assertThat(secondPage.data.map { it.id }).containsExactly(newest.id)
        assertThat(secondPage.hasNext).isFalse()
    }

    @Test
    fun `조회순도 포지션 필터를 함께 적용한다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val backend = createPost(author.id, team.id).also { createRecruitment(it.id) }
        val frontend = createPost(author.id, team.id).also { createRecruitment(it.id, Position.FRONTEND) }
        jdbcTemplate.update("UPDATE posts SET view_count = ? WHERE id = ?", 99, frontend.id)

        val response =
            postService.getPosts(
                PostGetQuery(positions = setOf(Position.BACKEND), sort = PostSort.MOST_VIEWED),
                CursorGetQuery(cursor = null),
                null,
            )

        assertThat(response.data.map { it.id }).containsExactly(backend.id)
    }

    @Test
    fun `마감 임박순도 포지션 필터를 함께 적용한다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val backend = openPost(author.id, team.id, LocalDate.now().plusDays(7))
        val frontend = createPost(author.id, team.id).also { createRecruitment(it.id, Position.FRONTEND) }

        val response =
            postService.getPosts(
                PostGetQuery(positions = setOf(Position.BACKEND), sort = PostSort.DEADLINE_SOON),
                CursorGetQuery(cursor = null),
                null,
            )

        assertThat(response.data.map { it.id }).containsExactly(backend.id)
        assertThat(response.data.map { it.id }).doesNotContain(frontend.id)
    }

    @Test
    fun `모집 정보가 없는 글은 마감 묶음으로 간다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val recruiting = openPost(author.id, team.id, null)
        val withoutRecruitment = createPost(author.id, team.id)

        val response = deadlineSoon(size = 20)

        assertThat(response.data.map { it.id }).containsExactly(recruiting.id, withoutRecruitment.id)
    }

    @Test
    fun `마감 임박순은 커서 글이 삭제돼도 다음 페이지가 이어진다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val soon = openPost(author.id, team.id, LocalDate.now().plusDays(1))
        val later = openPost(author.id, team.id, LocalDate.now().plusDays(7))

        val firstPage = deadlineSoon(size = 1)
        postService.deletePost(soon.id, author)

        assertThat(deadlineSoon(size = 20, cursor = firstPage.nextCursor).data.map { it.id })
            .containsExactly(later.id)
    }

    @Test
    fun `조회순은 커서 글이 삭제돼도 다음 페이지가 이어진다`() {
        val author = createUser("author")
        val team = createTeam(author.id)

        val mostViewed = createPost(author.id, team.id)
        val leastViewed = createPost(author.id, team.id)
        jdbcTemplate.update("UPDATE posts SET view_count = ? WHERE id = ?", 9, mostViewed.id)

        val firstPage = list(PostSort.MOST_VIEWED, size = 1)
        postService.deletePost(mostViewed.id, author)

        assertThat(list(PostSort.MOST_VIEWED, size = 20, cursor = firstPage.nextCursor).data.map { it.id })
            .containsExactly(leastViewed.id)
    }

    @Test
    fun `좋아요순은 커서 글이 삭제돼도 좋아요 있는 구간을 건너뛰지 않는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val firstLiker = createUser("first-liker")
        val secondLiker = createUser("second-liker")

        val noLike = createPost(author.id, team.id)
        val oneLike = createPost(author.id, team.id)
        val twoLikes = createPost(author.id, team.id)
        createLike(firstLiker.id, LikeType.POST, oneLike.id)
        createLike(firstLiker.id, LikeType.POST, twoLikes.id)
        createLike(secondLiker.id, LikeType.POST, twoLikes.id)

        val firstPage = list(PostSort.MOST_LIKED, size = 1)
        postService.deletePost(twoLikes.id, author)

        assertThat(list(PostSort.MOST_LIKED, size = 20, cursor = firstPage.nextCursor).data.map { it.id })
            .containsExactly(oneLike.id, noLike.id)
    }

    private fun list(
        sort: PostSort,
        size: Int,
        cursor: String? = null,
    ) = postService.getPosts(
        PostGetQuery(sort = sort),
        CursorGetQuery(cursor = cursor, size = size),
        null,
    )

    private fun deadlineSoon(
        size: Int,
        cursor: String? = null,
    ) = postService.getPosts(
        PostGetQuery(sort = PostSort.DEADLINE_SOON),
        CursorGetQuery(cursor = cursor, size = size),
        null,
    )

    private fun createRecruitment(
        postId: Long,
        position: Position,
    ): Recruitment = recruitmentRepository.save(Recruitment(position = position, count = 1, postId = postId))

    private fun openPost(
        userId: UUID,
        teamId: Long,
        deadline: LocalDate?,
    ): Post =
        postRepository
            .save(
                Post(
                    title = "title",
                    content = "content",
                    userId = userId,
                    teamId = teamId,
                    expiresAt = deadline?.let { Deadline.toExpiresAt(it) },
                ),
            ).also { createRecruitment(it.id) }

    private fun closedPost(
        userId: UUID,
        teamId: Long,
        deadline: LocalDate?,
    ): Post =
        // 트랜잭션 밖이라 detached 상태여서 close()만으로는 DB에 반영되지 않음
        openPost(userId, teamId, deadline).also { post ->
            recruitmentRepository.saveAll(
                recruitmentRepository.findByPostId(post.id).onEach { it.close() },
            )
        }
}
