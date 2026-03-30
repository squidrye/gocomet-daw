package com.ridehailing.core_api.common.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

  private val log = LoggerFactory.getLogger(this::class.java)

  @ExceptionHandler(AppException::class)
  fun handleAppException(ex: AppException): ResponseEntity<ErrorResponse> {
    log.debug("handleAppException - code=${ex.code}, message=${ex.message}")
    val response = ErrorResponse().apply {
      error = ex.message
      details = ex.details
    }
    return ResponseEntity.status(ex.status).body(response)
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    log.debug("handleValidation - ${ex.message}")
    val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
    val (code, status, message) = AppExceptionTypes.VALIDATION_FAILED
    val response = ErrorResponse().apply {
      this.error = message
      this.details = errors
    }
    return ResponseEntity.status(status).body(response)
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.debug("handleUnreadable - ${ex.message}")
    val (_, status, message) = AppExceptionTypes.MALFORMED_REQUEST
    val response = ErrorResponse().apply { error = message }
    return ResponseEntity.status(status).body(response)
  }
}
