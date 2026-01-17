package io.waggle.waggleapiserver.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "Invalid input value"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "Invalid type value"),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "Invalid state for this operation"),

    // 401 Unauthorized
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired token"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),

    // 404 Not Found
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),

    // 409 Conflict
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Resource already exists"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
}
