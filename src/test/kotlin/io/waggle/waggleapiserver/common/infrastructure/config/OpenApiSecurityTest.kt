package io.waggle.waggleapiserver.common.infrastructure.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.waggle.waggleapiserver.support.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@AutoConfigureMockMvc
class OpenApiSecurityTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    private fun apiDocs(): JsonNode =
        objectMapper.readTree(
            mockMvc
                .perform(get("/v3/api-docs"))
                .andReturn()
                .response.contentAsByteArray,
        )

    @Test
    fun `전역 기본값은 bearer 필수 인증임`() {
        val apiDocs = apiDocs()

        assertThat(apiDocs.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer")
        assertThat(apiDocs.at("/security")).isEqualTo(objectMapper.readTree("""[{"bearerAuth":[]}]"""))
        // security 필드가 없어야 전역 기본값을 상속함
        assertThat(apiDocs.at("/paths/~1posts/post").has("security")).isFalse()
    }

    @Test
    fun `SecurityRequirements가 붙은 공개 엔드포인트는 빈 배열로 전역 인증을 해제함`() {
        val apiDocs = apiDocs()

        assertThat(apiDocs.at("/paths/~1auth~1refresh/post/security")).isEqualTo(objectMapper.readTree("[]"))
        assertThat(apiDocs.at("/paths/~1users~1{userId}/get/security")).isEqualTo(objectMapper.readTree("[]"))
    }

    @Test
    fun `nullable CurrentUser를 받는 엔드포인트는 빈 요구사항이 포함된 optional 인증임`() {
        val apiDocs = apiDocs()
        val optionalSecurity = objectMapper.readTree("""[{"bearerAuth":[]},{}]""")

        assertThat(apiDocs.at("/paths/~1posts/get/security")).isEqualTo(optionalSecurity)
        assertThat(apiDocs.at("/paths/~1teams~1{teamId}/get/security")).isEqualTo(optionalSecurity)
    }
}
