package io.waggle.waggleapiserver.common.storage

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import java.util.UUID

// 이미지를 바꾸면 이전 이미지를 S3에서 지우므로 남이 올린 객체의 URL을 저장하면 그 객체가 지워짐
// 지금 값은 그대로 허용해 과거 형식 URL과 OAuth 가입 시 받은 외부 URL을 유지함
// 인터페이스 메서드로 두면 테스트 목이 검증을 삼키므로 판정만 isUploadedBy에 맡김
fun StorageClient.checkImageOwnership(
    imageUrl: String?,
    currentImageUrl: String?,
    directory: ImageDirectory,
    uploaderId: UUID,
) {
    if (imageUrl == null || imageUrl == currentImageUrl) {
        return
    }
    if (!isUploadedBy(imageUrl, directory, uploaderId)) {
        throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Image was not uploaded by the user")
    }
}
