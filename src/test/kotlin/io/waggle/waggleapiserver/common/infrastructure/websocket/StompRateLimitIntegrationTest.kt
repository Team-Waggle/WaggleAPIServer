package io.waggle.waggleapiserver.common.infrastructure.websocket

import io.waggle.waggleapiserver.support.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.support.AbstractSubscribableChannel

class StompRateLimitIntegrationTest
    @Autowired
    constructor(
        @Qualifier("clientInboundChannel") private val clientInboundChannel: AbstractSubscribableChannel,
    ) : IntegrationTestSupport() {
        @Test
        fun `StompRateLimitInterceptor가 client inbound channel에 등록됨`() {
            assertThat(clientInboundChannel.interceptors)
                .anyMatch { it is StompRateLimitInterceptor }
        }
    }
