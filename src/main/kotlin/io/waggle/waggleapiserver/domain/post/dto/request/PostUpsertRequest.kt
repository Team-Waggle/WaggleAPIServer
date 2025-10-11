package io.waggle.waggleapiserver.domain.post.dto.request

import jakarta.validation.constraints.NotBlank

data class PostUpsertRequest(
    @field:NotBlank val title: String,
    @field:NotBlank val content: String,
)
