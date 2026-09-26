package io.waggle.waggleapiserver.security.config

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.support.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.UUID

@AutoConfigureMockMvc
class SecurityPermitAllTest : IntegrationTestSupport() {
    companion object {
        private const val APPLICATION_PACKAGE = "io.waggle.waggleapiserver"
        private const val SAMPLE_UUID = "01900000-0000-7000-8000-000000000000"
        private const val SAMPLE_LONG_ID = "1"
        private val PATH_VARIABLE_PATTERN = Regex("""\{([^}:]+)[^}]*}""")
    }

    private data class Endpoint(
        val method: HttpMethod,
        val path: String,
        val markedPublic: Boolean,
    )

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    private fun anonymousStatus(
        method: HttpMethod,
        path: String,
    ): Int =
        mockMvc
            .perform(request(method, path))
            .andReturn()
            .response.status

    // Swagger의 공개 표시(OpenApiConfig, 빈 @SecurityRequirements)와 같은 기준이어야 문서와 실제 동작의 불일치를 잡음
    private fun isMarkedPublic(handlerMethod: HandlerMethod): Boolean =
        handlerMethod.methodParameters.any { it.hasParameterAnnotation(CurrentUser::class.java) && it.isOptional } ||
            handlerMethod.getMethodAnnotation(SecurityRequirements::class.java)?.value?.isEmpty() == true

    private fun samplePath(
        pattern: String,
        handlerMethod: HandlerMethod,
    ): String {
        val pathVariableTypeByName =
            handlerMethod.methodParameters
                .filter { it.hasParameterAnnotation(PathVariable::class.java) }
                .associate {
                    it.initParameterNameDiscovery(parameterNameDiscoverer)
                    it.parameterName to it.parameterType
                }
        return PATH_VARIABLE_PATTERN.replace(pattern) {
            if (pathVariableTypeByName[it.groupValues[1]] == UUID::class.java) SAMPLE_UUID else SAMPLE_LONG_ID
        }
    }

    private fun applicationEndpoints(): List<Endpoint> =
        handlerMapping.handlerMethods
            .filter { (_, handlerMethod) -> handlerMethod.beanType.packageName.startsWith(APPLICATION_PACKAGE) }
            .flatMap { (mappingInfo, handlerMethod) ->
                val requestMethods = mappingInfo.methodsCondition.methods.ifEmpty { setOf(RequestMethod.GET) }
                requestMethods.flatMap { requestMethod ->
                    mappingInfo.patternValues.map { pattern ->
                        Endpoint(
                            method = HttpMethod.valueOf(requestMethod.name),
                            path = samplePath(pattern, handlerMethod),
                            markedPublic = isMarkedPublic(handlerMethod),
                        )
                    }
                }
            }

    @Test
    fun `공개 표시가 있는 엔드포인트만 비로그인으로 열림`() {
        val endpoints = applicationEndpoints()

        val mismatches =
            endpoints.mapNotNull { endpoint ->
                val actuallyPublic = anonymousStatus(endpoint.method, endpoint.path) != 401
                if (actuallyPublic == endpoint.markedPublic) {
                    null
                } else {
                    "${endpoint.method} ${endpoint.path}: 공개 표시=${endpoint.markedPublic}, 실제 공개=$actuallyPublic"
                }
            }

        assertThat(endpoints).isNotEmpty()
        assertThat(mismatches).isEmpty()
    }

    @Test
    fun `공개 목록에 없는 GET 경로는 기본으로 로그인이 필요함`() {
        assertThat(anonymousStatus(HttpMethod.GET, "/users/$SAMPLE_UUID/unlisted")).isEqualTo(401)
        assertThat(anonymousStatus(HttpMethod.GET, "/teams/1/unlisted")).isEqualTo(401)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/posts/drafts", "/teams/unlisted", "/users/unlisted", "/users/bad"])
    fun `ID 자리에 ID 형식이 아닌 값이 오면 공개 경로로 취급하지 않음`(path: String) {
        assertThat(anonymousStatus(HttpMethod.GET, path)).isEqualTo(401)
    }
}
