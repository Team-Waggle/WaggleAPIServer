package io.waggle.waggleapiserver.common.exception

import io.waggle.waggleapiserver.common.infrastructure.discord.DiscordWebhookClient
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.ConstraintViolationException
import org.hibernate.exception.DataException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.sql.SQLException

class GlobalExceptionHandlerTest {
    private val discordWebhookClient = mock(DiscordWebhookClient::class.java)

    @Suppress("UNCHECKED_CAST")
    private val handler =
        GlobalExceptionHandler(
            (mock(ObjectProvider::class.java) as ObjectProvider<DiscordWebhookClient>).also {
                `when`(it.getIfAvailable()).thenReturn(discordWebhookClient)
            },
        )

    private val request = MockHttpServletRequest("PUT", "/users/me")

    @Test
    fun `유니크 제약 위반은 409로 응답하고 알림을 보내지 않는다`() {
        val sqlException = SQLException("Duplicate entry 'taken' for key 'users.uk_users_username'", "23000", 1062)
        val exception =
            DataIntegrityViolationException(
                "could not execute statement",
                ConstraintViolationException(
                    "could not execute statement",
                    sqlException,
                    "insert into users ...",
                    ConstraintViolationException.ConstraintKind.UNIQUE,
                    "users.uk_users_username",
                ),
            )

        val response = handler.handleDataIntegrityViolationException(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body!!.code).isEqualTo(ErrorCode.DUPLICATE_RESOURCE.name)
        assertThat(response.body!!.detail).isNull()
        verifyNoInteractions(discordWebhookClient)
    }

    @Test
    fun `유니크 제약이 아닌 무결성 위반은 500으로 응답하고 알림을 보낸다`() {
        val exception =
            DataIntegrityViolationException(
                "could not execute statement",
                DataException("could not execute statement", SQLException("Data too long for column 'username'")),
            )

        val response = handler.handleDataIntegrityViolationException(exception, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body!!.code).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name)
        assertThat(mockingDetails(discordWebhookClient).invocations).hasSize(1)
    }
}
