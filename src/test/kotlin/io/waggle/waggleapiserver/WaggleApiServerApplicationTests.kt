package io.waggle.waggleapiserver

import io.waggle.waggleapiserver.support.IntegrationTestSupport
import org.junit.jupiter.api.Test

// CascadeIntegrationTestSupport 는 S3·Auth 를 목으로 대체하므로 실제 빈 그래프 결함을 못 잡음
class WaggleApiServerApplicationTests : IntegrationTestSupport() {
    @Test
    fun contextLoads() {
    }
}
