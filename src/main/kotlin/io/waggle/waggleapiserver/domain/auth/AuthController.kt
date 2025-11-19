package io.waggle.waggleapiserver.domain.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.domain.auth.dto.response.AccessTokenResponse
import io.waggle.waggleapiserver.domain.auth.service.AuthService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증 토큰")
@RequestMapping("/auth")
@RestController
class AuthController(
    private val authService: AuthService,
) {
    @Operation(
        summary = "액세스 토큰 재발급",
        description = "유효한 리프레시 토큰으로 액세스 토큰을 재발급함",
    )
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refreshToken") refreshToken: String,
    ): AccessTokenResponse = authService.refresh(refreshToken)

    @Operation(
        summary = "로그아웃",
        description = "리프레시 토큰 무효화",
    )
    @PostMapping("/logout")
    fun logout(
        @CookieValue("refreshToken") refreshToken: String,
        response: HttpServletResponse,
    ) {
        authService.logout(refreshToken, response)
    }
}
