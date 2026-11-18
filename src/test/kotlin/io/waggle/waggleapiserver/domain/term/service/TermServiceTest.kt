package io.waggle.waggleapiserver.domain.term.service

import io.waggle.waggleapiserver.domain.term.TermType
import io.waggle.waggleapiserver.domain.term.dto.request.TermAgreementRequest
import io.waggle.waggleapiserver.domain.term.dto.request.TermsAgreeRequest
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class TermServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var termService: TermService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private val executor = Executors.newFixedThreadPool(2)

    private val request =
        TermsAgreeRequest(
            agreements =
                listOf(
                    TermAgreementRequest(type = TermType.SERVICE, agreed = true),
                    TermAgreementRequest(type = TermType.MARKETING, agreed = true),
                ),
        )

    @BeforeEach
    fun insertTerms() {
        jdbcTemplate.update(
            """
            INSERT INTO terms (type, version, content_url, mandatory, activated_at, created_at)
            VALUES ('SERVICE', 1, 'https://example.com/service', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                   ('MARKETING', 1, 'https://example.com/marketing', FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
            """.trimIndent(),
        )
    }

    @AfterEach
    fun shutdownExecutor() {
        executor.shutdownNow()
    }

    @Test
    fun `같은 약관에 다시 동의해도 동의 기록은 약관마다 하나다`() {
        val user = createUser("user")

        termService.agreeToTerms(request, user)
        termService.agreeToTerms(request, user)

        assertThat(count("SELECT COUNT(*) FROM user_term_agreements")).isEqualTo(2L)
    }

    @Test
    fun `같은 약관 동의가 동시에 들어와도 모두 성공하고 동의 기록은 약관마다 하나다`() {
        val user = createUser("user")
        val inserted = CountDownLatch(1)
        val release = CountDownLatch(1)

        val first =
            executor.submit {
                transactionTemplate.executeWithoutResult {
                    termService.agreeToTerms(request, user)
                    inserted.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
            }
        assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue()

        val second = executor.submit { termService.agreeToTerms(request, user) }

        // 두 번째 요청이 존재 확인을 지나 INSERT에서 첫 요청의 잠금을 기다리는 상태를 만듦
        assertThatThrownBy { second.get(500, TimeUnit.MILLISECONDS) }
            .isInstanceOf(TimeoutException::class.java)

        release.countDown()
        first.get(10, TimeUnit.SECONDS)
        second.get(10, TimeUnit.SECONDS)

        assertThat(count("SELECT COUNT(*) FROM user_term_agreements")).isEqualTo(2L)
    }
}
