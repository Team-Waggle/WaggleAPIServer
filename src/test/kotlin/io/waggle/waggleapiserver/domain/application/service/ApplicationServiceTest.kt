package io.waggle.waggleapiserver.domain.application.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.member.MemberRole
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
}
