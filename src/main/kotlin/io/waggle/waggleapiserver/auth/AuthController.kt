package io.waggle.waggleapiserver.auth

import io.waggle.waggleapiserver.auth.dto.response.AccessTokenResponse
import io.waggle.waggleapiserver.auth.service.AuthService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/auth")
@RestController
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refreshToken") refreshToken: String,
    ): AccessTokenResponse = authService.refresh(refreshToken)

    @PostMapping("/logout")
    fun logout(
        @CookieValue("refreshToken") refreshToken: String,
        response: HttpServletResponse,
    ) {
        authService.logout(refreshToken, response)
    }
}
