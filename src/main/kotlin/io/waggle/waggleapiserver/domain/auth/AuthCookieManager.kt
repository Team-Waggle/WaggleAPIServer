package io.waggle.waggleapiserver.domain.auth

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AuthCookieManager(
    @Value("\${app.cookie.secure}") private val cookieSecure: Boolean,
    @Value("\${app.cookie.same-site}") private val cookieSameSite: String,
    @Value("\${app.cookie.domain}") private val cookieDomain: String?,
) {
    fun addRefreshTokenCookie(
        response: HttpServletResponse,
        token: String,
        maxAgeSeconds: Int,
    ) {
        val cookie =
            Cookie("refreshToken", token).apply {
                isHttpOnly = true
                secure = cookieSecure
                path = "/auth"
                maxAge = maxAgeSeconds
                cookieDomain?.takeIf { it.isNotBlank() }?.let { domain = it }
            }

        response.addCookie(cookie)

        val header =
            buildString {
                append("refreshToken=$token; HttpOnly; Path=/auth; Max-Age=$maxAgeSeconds; ")
                if (cookieSecure) append("Secure; ")
                cookieDomain?.takeIf { it.isNotBlank() }?.let { append("Domain=$it; ") }
                append("SameSite=$cookieSameSite")
            }

        response.addHeader("Set-Cookie", header)
    }

    fun expireRefreshTokenCookie(response: HttpServletResponse) {
        val cookie =
            Cookie("refreshToken", null).apply {
                isHttpOnly = true
                secure = cookieSecure
                path = "/auth"
                maxAge = 0
                cookieDomain?.takeIf { it.isNotBlank() }?.let { domain = it }
            }

        response.addCookie(cookie)

        val header =
            buildString {
                append("refreshToken=; HttpOnly; Path=/auth; Max-Age=0; ")
                if (cookieSecure) append("Secure; ")
                cookieDomain?.takeIf { it.isNotBlank() }?.let { append("Domain=$it; ") }
                append("SameSite=$cookieSameSite")
            }

        response.addHeader("Set-Cookie", header)
    }
}
