package io.waggle.waggleapiserver.common.storage

data class GeneratedUploadUrl(
    val presignedUrl: String,
    val objectUrl: String,
)
