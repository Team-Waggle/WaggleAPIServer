package io.waggle.waggleapiserver.common.exception

class BusinessException(
    val errorCode: ErrorCode,
    message: String? = null,
) : RuntimeException(message ?: errorCode.message) {
    companion object {
        fun of(errorCode: ErrorCode): BusinessException = BusinessException(errorCode)

        fun of(
            errorCode: ErrorCode,
            message: String,
        ): BusinessException = BusinessException(errorCode, message)
    }
}
