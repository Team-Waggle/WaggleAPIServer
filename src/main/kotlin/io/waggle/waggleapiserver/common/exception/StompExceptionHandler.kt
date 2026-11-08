package io.waggle.waggleapiserver.common.exception

import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import io.waggle.waggleapiserver.common.util.logger
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.web.bind.annotation.ControllerAdvice

// GlobalExceptionHandler는 REST 전용이라 여기서 잡지 않으면 STOMP 실패가 로그에만 남음
// 보낸 쪽 화면은 전송된 것처럼 보이다가 새로고침하면 메시지가 사라짐
@ControllerAdvice
class StompExceptionHandler {
    @MessageExceptionHandler
    @SendToUser(ERROR_DESTINATION, broadcast = false)
    fun handleBusinessException(e: BusinessException): ErrorResponse {
        logger.warn("STOMP business exception occurred: ${e.errorCode} - ${e.message}")
        return ErrorResponse(
            status = e.errorCode.status.value(),
            code = e.errorCode.name,
            message = e.errorCode.message,
            detail = e.message,
        )
    }

    @MessageExceptionHandler
    @SendToUser(ERROR_DESTINATION, broadcast = false)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ErrorResponse {
        val detail =
            e.bindingResult
                ?.fieldErrors
                ?.joinToString(", ") { "${it.field}: ${it.defaultMessage ?: "validation failed"}" }
        logger.warn("STOMP validation failed: $detail")
        return ErrorResponse(
            status = ErrorCode.INVALID_INPUT_VALUE.status.value(),
            code = ErrorCode.INVALID_INPUT_VALUE.name,
            message = ErrorCode.INVALID_INPUT_VALUE.message,
            detail = detail,
        )
    }

    companion object {
        private const val ERROR_DESTINATION = "/queue/errors"
    }
}
