package io.waggle.waggleapiserver.domain.post.dto.request

import jakarta.validation.constraints.NotBlank

data class PostUpsertRequest(
    val projectId: Long?,
    @field:NotBlank val title: String,
    @field:NotBlank val content: String,
)
