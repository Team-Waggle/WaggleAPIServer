package io.waggle.waggleapiserver.domain.like.service

import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LikeCountTest : CascadeIntegrationTestSupport() {
    private fun postLikeCount(postId: Long): Long = count("SELECT like_count FROM posts WHERE id = ?", postId)

    private fun commentLikeCount(commentId: Long): Long =
        count("SELECT like_count FROM comments WHERE id = ?", commentId)

    private fun runConcurrently(
        threadCount: Int,
        action: () -> Unit,
    ) {
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)
        val futures =
            (1..threadCount).map {
                executor.submit {
                    startLatch.await()
                    action()
                }
            }
        startLatch.countDown()
        futures.forEach { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()
    }

    @Test
    fun `모집글 좋아요와 취소를 반복해도 카운터와 응답이 실제 행 수와 같음`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val post = createPost(author.id, createTeam(author.id).id)

        val firstLike = likeService.like(LikeType.POST, post.id, liker)
        val secondLike = likeService.like(LikeType.POST, post.id, liker)
        assertThat(firstLike.response.likeCount).isEqualTo(1L)
        assertThat(secondLike.response.likeCount).isEqualTo(1L)
        assertThat(postLikeCount(post.id)).isEqualTo(1L)

        val firstUnlike = likeService.unlike(LikeType.POST, post.id, liker)
        val secondUnlike = likeService.unlike(LikeType.POST, post.id, liker)
        assertThat(firstUnlike.likeCount).isZero()
        assertThat(secondUnlike.likeCount).isZero()
        assertThat(postLikeCount(post.id)).isZero()
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `댓글 좋아요와 취소도 카운터에 반영됨`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val post = createPost(author.id, createTeam(author.id).id)
        val comment = createComment(post.id, author.id)

        assertThat(likeService.like(LikeType.COMMENT, comment.id, liker).response.likeCount).isEqualTo(1L)
        assertThat(commentLikeCount(comment.id)).isEqualTo(1L)

        assertThat(likeService.unlike(LikeType.COMMENT, comment.id, liker).likeCount).isZero()
        assertThat(commentLikeCount(comment.id)).isZero()
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `좋아요 전에 읽어 둔 엔티티를 저장해도 카운터를 덮어쓰지 않음`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val post = createPost(author.id, createTeam(author.id).id)
        val comment = createComment(post.id, author.id)
        val stalePost = postRepository.findByIdOrNull(post.id)!!
        val staleComment = commentRepository.findByIdOrNull(comment.id)!!

        likeService.like(LikeType.POST, post.id, liker)
        likeService.like(LikeType.COMMENT, comment.id, liker)
        postRepository.save(stalePost)
        commentRepository.save(staleComment)

        assertThat(postLikeCount(post.id)).isEqualTo(1L)
        assertThat(commentLikeCount(comment.id)).isEqualTo(1L)
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `같은 좋아요를 동시에 눌러도 카운터는 한 번만 오름`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val post = createPost(author.id, createTeam(author.id).id)

        runConcurrently(threadCount = 8) { likeService.like(LikeType.POST, post.id, liker) }

        assertThat(postLikeCount(post.id)).isEqualTo(1L)
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `같은 좋아요를 동시에 취소해도 카운터는 한 번만 내려감`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val post = createPost(author.id, createTeam(author.id).id)
        likeService.like(LikeType.POST, post.id, liker)

        runConcurrently(threadCount = 8) { likeService.unlike(LikeType.POST, post.id, liker) }

        assertThat(postLikeCount(post.id)).isZero()
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `tombstone된 댓글은 카운터가 0이 되고 응답에도 0으로 나감`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val otherLiker = createUser("other-liker")
        val post = createPost(author.id, createTeam(author.id).id)
        val parent = createComment(post.id, author.id)
        createComment(post.id, liker.id, parentId = parent.id)
        likeService.like(LikeType.COMMENT, parent.id, liker)
        likeService.like(LikeType.COMMENT, parent.id, otherLiker)

        commentService.deleteComment(parent.id, author)

        assertThat(count("SELECT COUNT(*) FROM comments WHERE id = ? AND tombstoned_at IS NOT NULL", parent.id))
            .isEqualTo(1L)
        assertThat(commentLikeCount(parent.id)).isZero()
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `모집글을 지우면 글과 그 댓글의 카운터가 0이 됨`() {
        val owner = createUser("owner")
        val liker = createUser("liker")
        val team = createTeam(owner.id)
        val post = createPost(owner.id, team.id)
        val sibling = createPost(owner.id, team.id)
        val comment = createComment(post.id, owner.id)
        likeService.like(LikeType.POST, post.id, liker)
        likeService.like(LikeType.COMMENT, comment.id, liker)
        likeService.like(LikeType.POST, sibling.id, liker)

        postService.deletePost(post.id, owner)

        assertThat(postLikeCount(post.id)).isZero()
        assertThat(commentLikeCount(comment.id)).isZero()
        assertThat(postLikeCount(sibling.id)).isEqualTo(1L)
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `팀을 지우면 팀 모집글과 그 댓글의 카운터가 0이 됨`() {
        val leader = createUser("leader")
        val liker = createUser("liker")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        val comment = createComment(post.id, leader.id)
        val otherTeam = createTeam(liker.id)
        val otherPost = createPost(liker.id, otherTeam.id)
        likeService.like(LikeType.POST, post.id, liker)
        likeService.like(LikeType.COMMENT, comment.id, liker)
        likeService.like(LikeType.POST, otherPost.id, leader)

        teamService.deleteTeam(team.id, leader)

        assertThat(postLikeCount(post.id)).isZero()
        assertThat(commentLikeCount(comment.id)).isZero()
        assertThat(postLikeCount(otherPost.id)).isEqualTo(1L)
        assertThat(countLikeCountMismatches()).isZero()
    }

    @Test
    fun `탈퇴하면 탈퇴자가 누른 만큼 남의 카운터가 줄고 본인 글과 댓글은 0이 됨`() {
        val user = createUser("user")
        val other = createUser("other")
        val bystander = createUser("bystander")
        val team = createTeam(other.id)
        val othersPost = createPost(other.id, team.id)
        val othersComment = createComment(othersPost.id, other.id)
        val ownPost = createPost(user.id, team.id)
        val ownComment = createComment(othersPost.id, user.id)

        likeService.like(LikeType.POST, othersPost.id, user)
        likeService.like(LikeType.POST, othersPost.id, bystander)
        likeService.like(LikeType.COMMENT, othersComment.id, user)
        likeService.like(LikeType.COMMENT, othersComment.id, bystander)
        likeService.like(LikeType.POST, ownPost.id, user)
        likeService.like(LikeType.POST, ownPost.id, other)
        likeService.like(LikeType.COMMENT, ownComment.id, other)

        userService.deactivateUser(user)

        assertThat(postLikeCount(othersPost.id)).isEqualTo(1L)
        assertThat(commentLikeCount(othersComment.id)).isEqualTo(1L)
        assertThat(postLikeCount(ownPost.id)).isZero()
        assertThat(commentLikeCount(ownComment.id)).isZero()
        assertThat(countLikeCountMismatches()).isZero()
    }
}
