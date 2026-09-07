package io.waggle.waggleapiserver.domain.post

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpdateRequest
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpdateStatusRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PostDeadlineTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var applicationService: ApplicationService

    @Test
    fun `마감 기한이 없으면 무기한 모집이라 만료가 아니다`() {
        val post = createPostWithDeadline(null)

        assertThat(post.isExpired).isFalse()
    }

    @Test
    fun `마감 기한이 미래면 만료가 아니다`() {
        val post = createPostWithDeadline(Instant.now().plus(1, ChronoUnit.DAYS))

        assertThat(post.isExpired).isFalse()
    }

    @Test
    fun `배타적 경계라 마감 시각에 도달하면 만료다`() {
        val post = createPostWithDeadline(Instant.now())

        assertThat(post.isExpired).isTrue()
    }

    @Test
    fun `마감된 모집글은 포지션이 RECRUITING 이어도 모집 중이 아니다`() {
        val post = createPostWithDeadline(Instant.now().minus(1, ChronoUnit.DAYS))
        val recruitment = createRecruitment(post.id)

        val response = postService.getPost(post.id, null)

        assertThat(recruitment.isRecruiting()).isTrue()
        assertThat(response.recruiting).isFalse()
        assertThat(response.deadline).isEqualTo(Deadline.toDeadline(post.expiresAt!!))
    }

    @Test
    fun `마감되지 않은 모집글은 포지션 상태를 그대로 따른다`() {
        val post = createPostWithDeadline(Instant.now().plus(1, ChronoUnit.DAYS))
        createRecruitment(post.id)

        assertThat(postService.getPost(post.id, null).recruiting).isTrue()
    }

    @Test
    fun `마감된 모집글에는 지원할 수 없다`() {
        val post = createPostWithDeadline(Instant.now().minus(1, ChronoUnit.DAYS))
        createRecruitment(post.id)
        val applicant = createUser("applicant")

        assertThatThrownBy { apply(post.teamId, post.id, applicant) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
    }

    @Test
    fun `마감 기한이 없는 모집글에는 지원할 수 있다`() {
        val post = createPostWithDeadline(null)
        createRecruitment(post.id)
        val applicant = createUser("applicant")

        val response = apply(post.teamId, post.id, applicant)

        assertThat(response.postId).isEqualTo(post.id)
    }

    @Test
    fun `마감된 모집글도 상세 조회는 된다`() {
        val post = createPostWithDeadline(Instant.now().minus(1, ChronoUnit.DAYS))

        assertThat(postService.getPost(post.id, null).id).isEqualTo(post.id)
    }

    @Test
    fun `마감일을 과거로 옮길 수 없다`() {
        val post = createPostWithDeadline(Deadline.toExpiresAt(LocalDate.now().plusDays(7)))

        assertThatThrownBy {
            post.update("title", "content", Deadline.toExpiresAt(LocalDate.now().minusDays(1)))
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
    }

    @Test
    fun `마감된 모집글도 마감일을 그대로 두면 본문을 고칠 수 있다`() {
        val past = Deadline.toExpiresAt(LocalDate.now().minusDays(3))
        val post = createPostWithDeadline(past)

        post.update("새 제목", "새 내용", past)

        assertThat(post.title).isEqualTo("새 제목")
        assertThat(post.expiresAt).isEqualTo(past)
    }

    @Test
    fun `마감일 연장과 무기한 전환은 허용된다`() {
        val post = createPostWithDeadline(Deadline.toExpiresAt(LocalDate.now().minusDays(3)))

        val extended = Deadline.toExpiresAt(LocalDate.now().plusDays(7))
        post.update("title", "content", extended)
        assertThat(post.expiresAt).isEqualTo(extended)

        post.update("title", "content", null)
        assertThat(post.expiresAt).isNull()
    }

    @Test
    fun `마감된 모집글에는 포지션을 추가할 수 없다`() {
        val (manager, post) = createManagedPost(Deadline.toExpiresAt(LocalDate.now().minusDays(3)))
        createRecruitment(post.id)

        assertThatThrownBy {
            postService.updatePost(post.id, updateRequest(post, Position.FRONTEND), manager)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
    }

    @Test
    fun `기한이 남았으면 포지션을 추가할 수 있고 모집 중으로 들어간다`() {
        val future = LocalDate.now().plusDays(7)
        val (manager, post) = createManagedPost(Deadline.toExpiresAt(future))
        createRecruitment(post.id)

        val response = postService.updatePost(post.id, updateRequest(post, Position.FRONTEND, future), manager)

        assertThat(response.recruitments.map { it.position })
            .containsExactlyInAnyOrder(Position.BACKEND, Position.FRONTEND)
        assertThat(response.recruitments).allMatch { it.status == RecruitmentStatus.RECRUITING }
    }

    @Test
    fun `마감된 모집글도 기한을 연장하면서 포지션을 추가할 수 있다`() {
        val (manager, post) = createManagedPost(Deadline.toExpiresAt(LocalDate.now().minusDays(3)))
        createRecruitment(post.id)

        val response =
            postService.updatePost(
                post.id,
                updateRequest(post, Position.FRONTEND, LocalDate.now().plusDays(7)),
                manager,
            )

        assertThat(response.recruiting).isTrue()
        assertThat(response.recruitments).hasSize(2)
    }

    @Test
    fun `마감된 포지션은 삭제할 수 없다`() {
        val future = LocalDate.now().plusDays(7)
        val (manager, post) = createManagedPost(Deadline.toExpiresAt(future))
        createRecruitment(post.id)
        postService.updatePostRecruitmentStatus(
            post.id,
            RecruitmentUpdateStatusRequest(RecruitmentStatus.CLOSED),
            manager,
        )

        assertThatThrownBy {
            postService.updatePost(post.id, emptyRecruitmentRequest(post), manager)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
    }

    @Test
    fun `지원서가 있는 포지션은 삭제할 수 없다`() {
        val future = LocalDate.now().plusDays(7)
        val (manager, post) = createManagedPost(Deadline.toExpiresAt(future))
        createRecruitment(post.id)
        apply(post.teamId, post.id, createUser("applicant-${System.nanoTime()}"))

        assertThatThrownBy {
            postService.updatePost(post.id, emptyRecruitmentRequest(post, future), manager)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
    }

    private fun emptyRecruitmentRequest(
        post: Post,
        deadline: LocalDate? = post.expiresAt?.let { Deadline.toDeadline(it) },
    ) = PostUpdateRequest(
        title = post.title,
        content = post.content,
        recruitments = emptyList(),
        deadline = deadline,
    )

    private fun createManagedPost(expiresAt: Instant?): Pair<User, Post> {
        val manager = createUser("manager-${System.nanoTime()}")
        val team = createTeam(manager.id)
        createMember(manager.id, team.id, MemberRole.MANAGER)
        val post =
            postRepository.save(
                Post(
                    title = "title",
                    content = "content",
                    userId = manager.id,
                    teamId = team.id,
                    expiresAt = expiresAt,
                ),
            )
        return manager to post
    }

    private fun updateRequest(
        post: Post,
        addedPosition: Position,
        deadline: LocalDate? = post.expiresAt?.let { Deadline.toDeadline(it) },
    ) = PostUpdateRequest(
        title = post.title,
        content = post.content,
        recruitments =
            listOf(
                RecruitmentUpsertRequest(position = Position.BACKEND, count = 1, skills = setOf(Skill.KOTLIN)),
                RecruitmentUpsertRequest(position = addedPosition, count = 1, skills = setOf(Skill.KOTLIN)),
            ),
        deadline = deadline,
    )

    private fun createPostWithDeadline(expiresAt: Instant?): Post {
        val author = createUser("author-${System.nanoTime()}")
        val team = createTeam(author.id)
        return postRepository.save(
            Post(
                title = "title",
                content = "content",
                userId = author.id,
                teamId = team.id,
                expiresAt = expiresAt,
            ),
        )
    }

    private fun apply(
        teamId: Long,
        postId: Long,
        applicant: User,
    ) = applicationService.applyToTeam(
        teamId,
        ApplicationCreateRequest(postId = postId, position = Position.BACKEND),
        applicant,
    )
}
