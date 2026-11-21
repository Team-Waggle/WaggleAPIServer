package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.post.Deadline
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpdateRequest
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PostServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var applicationService: ApplicationService

    @Autowired
    private lateinit var memberService: MemberService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private val executor = Executors.newFixedThreadPool(2)

    @AfterEach
    fun shutdownExecutor() {
        executor.shutdownNow()
    }

    @Test
    fun `모집글 상세는 팀을 나가거나 추방된 사람만 이전 팀원으로 표시한다`() {
        val leader = createUser("leader")
        val member = createUser("member")
        val leaver = createUser("leaver")
        val removed = createUser("removed")
        val outsider = createUser("outsider")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(member.id, team.id, MemberRole.MEMBER)
        createMember(leaver.id, team.id, MemberRole.MEMBER)
        val removedMember = createMember(removed.id, team.id, MemberRole.MEMBER)
        val post = createPost(leader.id, team.id)
        memberService.leaveTeam(team.id, leaver)
        memberService.removeMember(removedMember.id, leader)

        assertThat(postService.getPost(post.id, leaver).formerMember).isTrue()
        assertThat(postService.getPost(post.id, removed).formerMember).isTrue()
        assertThat(postService.getPost(post.id, member).formerMember).isFalse()
        assertThat(postService.getPost(post.id, outsider).formerMember).isFalse()
        assertThat(postService.getPost(post.id, null).formerMember).isFalse()
    }

    @Test
    fun `팀을 떠난 작성자는 자기 모집글을 수정하거나 삭제할 수 없다`() {
        val leader = createUser("leader")
        val author = createUser("author")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val authorMember = createMember(author.id, team.id, MemberRole.MANAGER)
        val post = createPostWithRecruitment(author, team.id)
        memberService.removeMember(authorMember.id, leader)

        assertAccessDenied { postService.updatePost(post.id, updateRequest("수정"), author) }
        assertAccessDenied { postService.deletePost(post.id, author) }
        assertThat(postRepository.findById(post.id).get().title).isEqualTo("title")
    }

    @Test
    fun `팀장과 관리자는 남이 쓴 모집글을 수정하고 삭제할 수 있다`() {
        val leader = createUser("leader")
        val manager = createUser("manager")
        val author = createUser("author")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(manager.id, team.id, MemberRole.MANAGER)
        createMember(author.id, team.id, MemberRole.MEMBER)
        val post = createPostWithRecruitment(author, team.id)

        postService.updatePost(post.id, updateRequest("관리자 수정"), manager)
        assertThat(postRepository.findById(post.id).get().title).isEqualTo("관리자 수정")

        postService.deletePost(post.id, leader)
        assertThat(postRepository.findById(post.id)).isEmpty()
    }

    @Test
    fun `일반 팀원은 남이 쓴 모집글을 수정하거나 삭제할 수 없고 자기 글은 할 수 있다`() {
        val leader = createUser("leader")
        val member = createUser("member")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(member.id, team.id, MemberRole.MEMBER)
        val leaderPost = createPostWithRecruitment(leader, team.id)
        val memberPost = createPostWithRecruitment(member, team.id)

        assertAccessDenied { postService.updatePost(leaderPost.id, updateRequest("수정"), member) }
        assertAccessDenied { postService.deletePost(leaderPost.id, member) }

        postService.updatePost(memberPost.id, updateRequest("본인 수정"), member)
        assertThat(postRepository.findById(memberPost.id).get().title).isEqualTo("본인 수정")
    }

    @Test
    fun `모집 인원은 승인 인원보다 적게 줄일 수 없고 같게 줄이면 그 직무가 마감된다`() {
        val leader = createUser("leader")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        val recruitment =
            recruitmentRepository.save(
                Recruitment(position = Position.BACKEND, count = 3, postId = post.id),
            )
        listOf("first", "second").forEach {
            val application = createApplication(team.id, post.id, createUser(it).id)
            applicationService.updateApplicationStatus(application.id, ApplicationStatus.APPROVED, leader)
        }

        assertThatThrownBy { postService.updatePost(post.id, updateRequest("수정", count = 1), leader) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
        assertThat(recruitmentRepository.findById(recruitment.id).get().count).isEqualTo(3)

        postService.updatePost(post.id, updateRequest("수정", count = 2), leader)
        assertThat(recruitmentRepository.findById(recruitment.id).get().isRecruiting()).isFalse()
        assertThat(postRepository.findById(post.id).get().expiresAt).isEqualTo(Deadline.todayExpiresAt())
    }

    @Test
    fun `승인 중인 직무의 모집 인원을 줄이면 승인이 커밋된 뒤의 승인 인원으로 검사한다`() {
        val leader = createUser("leader")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        val recruitment =
            recruitmentRepository.save(
                Recruitment(position = Position.BACKEND, count = 3, postId = post.id),
            )
        val approved = createApplication(team.id, post.id, createUser("approved").id)
        applicationService.updateApplicationStatus(approved.id, ApplicationStatus.APPROVED, leader)
        val lockHeld = CountDownLatch(1)
        val release = CountDownLatch(1)

        val approval =
            executor.submit {
                transactionTemplate.executeWithoutResult {
                    recruitmentRepository.findForUpdateByPostIdAndPosition(post.id, Position.BACKEND)
                    recruitmentRepository.increaseApprovedCount(recruitment.id)
                    lockHeld.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
            }
        assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue()

        val update = executor.submit { postService.updatePost(post.id, updateRequest("수정", count = 1), leader) }

        // 잠금이 없으면 승인 1명 기준으로 통과해 정원 1에 승인 2가 남음
        assertThatThrownBy { update.get(500, TimeUnit.MILLISECONDS) }
            .isInstanceOf(TimeoutException::class.java)

        release.countDown()
        approval.get(10, TimeUnit.SECONDS)

        assertThatThrownBy { update.get(10, TimeUnit.SECONDS) }
            .isInstanceOf(ExecutionException::class.java)
            .cause()
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
        assertThat(recruitmentRepository.findById(recruitment.id).get().count).isEqualTo(3)
    }

    private fun createPostWithRecruitment(
        author: User,
        teamId: Long,
    ): Post = createPost(author.id, teamId).also { createRecruitment(it.id) }

    private fun updateRequest(
        title: String,
        count: Int = 1,
    ) = PostUpdateRequest(
        title = title,
        content = "content",
        recruitments =
            listOf(
                RecruitmentUpsertRequest(position = Position.BACKEND, count = count, skills = emptySet()),
            ),
        deadline = null,
    )

    private fun assertAccessDenied(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ACCESS_DENIED)
    }
}
