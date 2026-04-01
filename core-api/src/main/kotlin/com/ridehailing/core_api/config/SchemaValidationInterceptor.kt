package com.ridehailing.core_api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.ridehailing.core_api.common.util.SchemaValidator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.util.ContentCachingRequestWrapper

@Component
open class SchemaValidationInterceptor : HandlerInterceptor {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var schemaValidator: SchemaValidator

  private val objectMapper = ObjectMapper()

  private val schemaMap = mapOf(
    "POST:/v1/auth/register" to "RegisterRequest.json",
    "POST:/v1/auth/login" to "LoginRequest.json",
    "POST:/v1/rides" to "CreateRideRequest.json",
    "POST:/v1/payments" to "PaymentRequest.json"
  )

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val key = "${request.method}:${request.requestURI}"
    val schemaPath = schemaMap[key] ?: return true

    val wrapper = request as? ContentCachingRequestWrapper ?: return true
    val body = wrapper.contentAsByteArray
    if (body.isEmpty()) return true

    try {
      val jsonNode = objectMapper.readTree(body)
      schemaValidator.validate(schemaPath, jsonNode)
    } catch (e: com.ridehailing.core_api.common.exception.AppException) {
      throw e
    } catch (e: Exception) {
      log.debug("preHandle - failed to parse request body for schema validation: ${e.message}")
    }

    return true
  }
}
