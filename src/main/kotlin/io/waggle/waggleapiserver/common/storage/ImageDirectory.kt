package io.waggle.waggleapiserver.common.storage

// path는 S3 키의 첫 경로라 바꾸면 기존 이미지의 업로더 판정과 삭제가 어긋남
enum class ImageDirectory(
    val path: String,
) {
    USER_PROFILE("users"),
    TEAM_PROFILE("teams"),
    POST_CONTENT("posts"),
}
