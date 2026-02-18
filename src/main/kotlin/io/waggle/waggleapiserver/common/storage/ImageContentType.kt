package io.waggle.waggleapiserver.common.storage

enum class ImageContentType(
    val mediaType: String,
    val extension: String,
) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
}
