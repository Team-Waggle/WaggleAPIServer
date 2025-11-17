package io.waggle.waggleapiserver.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "액세스 토큰 응답 DTO")
data class AccessTokenResponse(
    @Schema(
        description = "엑세스 토큰",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    )
    val accessToken: String,
)
