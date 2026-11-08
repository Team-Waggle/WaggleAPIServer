package io.waggle.waggleapiserver.security.jwt

// 같은 키로 서명하므로 종류를 구분하지 않으면 유효 기간이 긴 리프레시 토큰으로 모든 API를 호출할 수 있음
enum class TokenType(
    val claimValue: String,
) {
    ACCESS("access"),
    REFRESH("refresh"),
}
