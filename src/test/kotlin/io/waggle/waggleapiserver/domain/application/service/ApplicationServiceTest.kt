package io.waggle.waggleapiserver.domain.application.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
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

class ApplicationServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var applicationService: ApplicationService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private val executor = Executors.newFixedThreadPool(2)

    @AfterEach
    fun shutdownExecutor() {
        executor.shutdownNow()
    }

    @Test
    fun `승인 중인 지원서를 거절하면 승인이 커밋될 때까지 기다린 뒤 거부된다`() {
        val leader = createUser("leader")
        val applicant = createUser("applicant")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        val application = createApplication(team.id, post.id, applicant.id)
        val lockHeld = CountDownLatch(1)
        val release = CountDownLatch(1)

        val approval =
            executor.submit {
                transactionTemplate.executeWithoutResult {
                    val locked = applicationRepository.findForUpdateById(application.id)!!
                    lockHeld.countDown()
                    release.await(10, TimeUnit.SECONDS)
                    locked.updateStatus(ApplicationStatus.APPROVED)
                }
            }
        assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue()

        val rejection =
            executor.submit {
                applicationService.updateApplicationStatus(application.id, ApplicationStatus.REJECTED, leader)
            }

        // 잠금이 없으면 거절이 바로 끝나 승인을 덮어씀
        assertThatThrownBy { rejection.get(500, TimeUnit.MILLISECONDS) }
            .isInstanceOf(TimeoutException::class.java)

        release.countDown()
        approval.get(10, TimeUnit.SECONDS)

        assertThatThrownBy { rejection.get(10, TimeUnit.SECONDS) }
            .isInstanceOf(ExecutionException::class.java)
            .cause()
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE)
        assertThat(applicationRepository.findById(application.id).get().status)
            .isEqualTo(ApplicationStatus.APPROVED)
    }

    @Test
    fun `취소 중인 지원서를 승인하면 취소가 커밋될 때까지 기다린 뒤 찾을 수 없다`() {
        val leader = createUser("leader")
        val applicant = createUser("applicant")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        val application = createApplication(team.id, post.id, applicant.id)
        val lockHeld = CountDownLatch(1)
        val release = CountDownLatch(1)

        val cancellation =
            executor.submit {
                transactionTemplate.executeWithoutResult {
                    val locked = applicationRepository.findForUpdateByIdAndUserId(application.id, applicant.id)!!
                    lockHeld.countDown()
                    release.await(10, TimeUnit.SECONDS)
                    locked.delete()
                }
            }
        assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue()

        val approval =
            executor.submit {
                applicationService.updateApplicationStatus(application.id, ApplicationStatus.APPROVED, leader)
            }

        assertThatThrownBy { approval.get(500, TimeUnit.MILLISECONDS) }
            .isInstanceOf(TimeoutException::class.java)

        release.countDown()
        cancellation.get(10, TimeUnit.SECONDS)

        assertThatThrownBy { approval.get(10, TimeUnit.SECONDS) }
            .isInstanceOf(ExecutionException::class.java)
            .cause()
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
        assertThat(
            count(
                "SELECT COUNT(*) FROM members WHERE user_id = UUID_TO_BIN(?) AND team_id = ?",
                applicant.id.toString(),
                team.id,
            ),
        ).isZero()
    }

    @Test
    fun `같은 모집글에 다른 포지션으로 동시에 지원하면 먼저 커밋된 한 건만 남는다`() {
        val leader = createUser("leader")
        val applicant = createUser("applicant")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        createRecruitment(post.id)
        recruitmentRepository.save(Recruitment(position = Position.FRONTEND, count = 1, postId = post.id))
        val inserted = CountDownLatch(1)
        val release = CountDownLatch(1)

        val backendApplication =
            executor.submit {
                transactionTemplate.executeWithoutResult {
                    createApplication(team.id, post.id, applicant.id)
                    inserted.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
            }
        assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue()

        val frontendApplication =
            executor.submit {
                applicationService.applyToTeam(
                    team.id,
                    ApplicationCreateRequest(postId = post.id, position = Position.FRONTEND),
                    applicant,
                )
            }

        // 유니크 인덱스가 없으면 커밋 전 지원이 보이지 않아 기다리지 않고 중복 지원이 들어감
        assertThatThrownBy { frontendApplication.get(500, TimeUnit.MILLISECONDS) }
            .isInstanceOf(TimeoutException::class.java)

        release.countDown()
        backendApplication.get(10, TimeUnit.SECONDS)

        assertThatThrownBy { frontendApplication.get(10, TimeUnit.SECONDS) }
            .isInstanceOf(ExecutionException::class.java)
            .cause()
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_RESOURCE)
        assertThat(count("SELECT COUNT(*) FROM applications WHERE post_id = ?", post.id)).isEqualTo(1L)
    }

    @Test
    fun `지원을 취소한 모집글에는 다시 지원할 수 있다`() {
        val leader = createUser("leader")
        val applicant = createUser("applicant")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        createRecruitment(post.id)
        val request = ApplicationCreateRequest(postId = post.id, position = Position.BACKEND)

        val first = applicationService.applyToTeam(team.id, request, applicant)
        applicationService.deleteApplication(first.id, applicant)
        applicationService.applyToTeam(team.id, request, applicant)

        assertThat(count("SELECT COUNT(*) FROM applications WHERE post_id = ? AND deleted_at IS NULL", post.id))
            .isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM applications WHERE post_id = ?", post.id)).isEqualTo(2L)
    }
}
