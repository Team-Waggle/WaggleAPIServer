package io.waggle.waggleapiserver.security.jwt

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class JwtUtil {
    fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return bearerToken?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
    }
}
