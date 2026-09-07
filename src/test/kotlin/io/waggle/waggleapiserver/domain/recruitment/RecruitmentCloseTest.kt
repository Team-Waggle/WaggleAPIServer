package io.waggle.waggleapiserver.domain.recruitment

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.post.Deadline
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpdateStatusRequest
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.time.LocalDate

class RecruitmentCloseTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var applicationService: ApplicationService

    @Test
    fun `마감은 모집글의 열린 포지션을 모두 닫는다`() {
        val (manager, post) = createManagedPost(null)
        createRecruitment(post.id)

        postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)

        assertThat(recruitmentRepository.findByPostId(post.id))
            .allMatch { it.status == RecruitmentStatus.CLOSED }
    }

    @Test
    fun `일부만 닫힌 상태에서도 나머지를 닫을 수 있다`() {
        val (manager, post) = createManagedPost(null)
        val alreadyClosed = createRecruitment(post.id)
        alreadyClosed.close()
        recruitmentRepository.save(alreadyClosed)
        saveRecruitment(post.id, Position.FRONTEND)

        postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)

        assertThat(recruitmentRepository.findByPostId(post.id))
            .allMatch { it.status == RecruitmentStatus.CLOSED }
    }

    @Test
    fun `이미 전부 닫힌 모집글을 또 닫으면 실패한다`() {
        val (manager, post) = createManagedPost(null)
        createRecruitment(post.id)
        postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)

        assertThatThrownBy {
            postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
    }

    @Test
    fun `마감된 모집을 다시 모집 중으로 되돌릴 수 없다`() {
        val (manager, post) = createManagedPost(null)
        createRecruitment(post.id)

        assertThatThrownBy {
            postService.updatePostRecruitmentStatus(
                post.id,
                RecruitmentUpdateStatusRequest(RecruitmentStatus.RECRUITING),
                manager,
            )
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
    }

    @Test
    fun `수동 마감은 마감일을 오늘로 당긴다`() {
        val (manager, post) = createManagedPost(Deadline.toExpiresAt(LocalDate.now().plusDays(7)))
        createRecruitment(post.id)

        postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)

        val closed = postRepository.findByIdOrNull(post.id)!!
        assertThat(closed.expiresAt).isEqualTo(Deadline.todayExpiresAt())
        assertThat(closed.isExpired).isFalse()
    }

    @Test
    fun `무기한 모집을 수동 마감하면 마감일이 오늘이 된다`() {
        val (manager, post) = createManagedPost(null)
        createRecruitment(post.id)

        postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)

        val closed = postRepository.findByIdOrNull(post.id)!!
        assertThat(closed.expiresAt).isEqualTo(Deadline.todayExpiresAt())
        assertThat(closed.isExpired).isFalse()
    }

    @Test
    fun `이미 지난 마감일은 수동 마감이 미래로 밀지 않는다`() {
        val past = Deadline.toExpiresAt(LocalDate.now().minusDays(3))
        val (manager, post) = createManagedPost(past)
        createRecruitment(post.id)

        postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)

        assertThat(postRepository.findByIdOrNull(post.id)!!.expiresAt).isEqualTo(past)
    }

    @Test
    fun `수동 마감 후에는 기한이 남아도 지원할 수 없다`() {
        val (manager, post) = createManagedPost(Deadline.toExpiresAt(LocalDate.now().plusDays(7)))
        createRecruitment(post.id)
        postService.updatePostRecruitmentStatus(post.id, closeRequest(), manager)
        val applicant = createUser("applicant-${System.nanoTime()}")

        assertThatThrownBy {
            applicationService.applyToTeam(
                post.teamId,
                ApplicationCreateRequest(postId = post.id, position = Position.BACKEND),
                applicant,
            )
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
    }

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

    private fun saveRecruitment(
        postId: Long,
        position: Position,
    ): Recruitment =
        recruitmentRepository.save(
            Recruitment(position = position, count = 1, postId = postId),
        )

    private fun closeRequest() = RecruitmentUpdateStatusRequest(RecruitmentStatus.CLOSED)
}
