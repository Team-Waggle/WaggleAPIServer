package io.waggle.waggleapiserver.domain.project.dto.response

data class ProjectSimpleResponse(
    val projectId: Long,
    val name: String,
    val description: String,
)
