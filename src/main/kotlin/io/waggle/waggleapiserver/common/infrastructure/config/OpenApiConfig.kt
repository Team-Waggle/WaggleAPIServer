package io.waggle.waggleapiserver.common.infrastructure.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.swagger.v3.oas.models.security.SecurityRequirement as SecurityRequirementModel

private const val BEARER_AUTH = "bearerAuth"

@Configuration
@OpenAPIDefinition(security = [SecurityRequirement(name = BEARER_AUTH)])
@SecurityScheme(
    name = BEARER_AUTH,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
)
class OpenApiConfig {
    // springdoc이 name이 빈 @SecurityRequirement를 버리므로 빈 요구사항은 모델 객체로만 추가 가능
    // 판별 기준은 CurrentUserArgumentResolver의 isOptional과 동일해야 함
    @Bean
    fun optionalAuthenticationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            val isAuthenticationOptional =
                handlerMethod.methodParameters.any {
                    it.hasParameterAnnotation(CurrentUser::class.java) && it.isOptional
                }
            if (isAuthenticationOptional) {
                operation.security =
                    listOf(SecurityRequirementModel().addList(BEARER_AUTH), SecurityRequirementModel())
            }
            operation
        }
}
