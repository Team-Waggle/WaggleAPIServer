package io.waggle.waggleapiserver.common.exception

import io.waggle.waggleapiserver.common.util.logger
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val detail: String? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn("Business exception occurred: ${e.errorCode}", e)
        val errorResponse =
            ErrorResponse(
                status = e.errorCode.status.value(),
                code = e.errorCode.name,
                message = e.errorCode.message,
                detail = e.message,
            )
        return ResponseEntity
            .status(e.errorCode.status)
            .body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.warn("Validation exception occurred", e)
        val errors =
            e.bindingResult.allErrors.joinToString(", ") { error ->
                val fieldName = (error as? FieldError)?.field ?: "unknown"
                val errorMessage = error.defaultMessage ?: "validation failed"
                "$fieldName: $errorMessage"
            }
        val errorResponse =
            ErrorResponse(
                status = ErrorCode.INVALID_INPUT_VALUE.status.value(),
                code = ErrorCode.INVALID_INPUT_VALUE.name,
                message = ErrorCode.INVALID_INPUT_VALUE.message,
                detail = errors,
            )
        return ResponseEntity
            .status(ErrorCode.INVALID_INPUT_VALUE.status)
            .body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.warn("Type mismatch exception occurred", e)
        val detail =
            "Invalid type for parameter '${e.name}': expected ${e.requiredType?.simpleName}"
        val errorResponse =
            ErrorResponse(
                status = ErrorCode.INVALID_TYPE_VALUE.status.value(),
                code = ErrorCode.INVALID_TYPE_VALUE.name,
                message = ErrorCode.INVALID_TYPE_VALUE.message,
                detail = detail,
            )
        return ResponseEntity
            .status(ErrorCode.INVALID_TYPE_VALUE.status)
            .body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception occurred", e)
        val errorResponse =
            ErrorResponse(
                status = ErrorCode.INTERNAL_SERVER_ERROR.status.value(),
                code = ErrorCode.INTERNAL_SERVER_ERROR.name,
                message = ErrorCode.INTERNAL_SERVER_ERROR.message,
                detail = e.message,
            )
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.status)
            .body(errorResponse)
    }
}
