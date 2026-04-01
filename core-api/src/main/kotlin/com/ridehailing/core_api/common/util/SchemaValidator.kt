package com.ridehailing.core_api.common.util

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
open class SchemaValidator {

  private val log = LoggerFactory.getLogger(this::class.java)
  private val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
  private val schemaCache = mutableMapOf<String, com.networknt.schema.JsonSchema>()

  fun validate(schemaPath: String, body: JsonNode) {
    val schema = schemaCache.getOrPut(schemaPath) {
      val resource = ClassPathResource("json_schema/$schemaPath")
      factory.getSchema(resource.inputStream)
    }

    if (containsXss(body)) {
      log.info("validate - XSS detected in request body")
      throw AppException(AppExceptionTypes.VALIDATION_FAILED, listOf("Request contains potentially unsafe content"))
    }

    val errors = schema.validate(body)
    if (errors.isNotEmpty()) {
      val messages = errors.map { it.message }
      log.info("validate - schema validation failed: $messages")
      throw AppException(AppExceptionTypes.VALIDATION_FAILED, messages)
    }
  }

  private fun containsXss(node: JsonNode): Boolean {
    if (node.isTextual) {
      return InputSanitizer.containsSuspiciousContent(node.textValue())
    }
    if (node.isObject) {
      return node.fields().asSequence().any { containsXss(it.value) }
    }
    if (node.isArray) {
      return node.elements().asSequence().any { containsXss(it) }
    }
    return false
  }
}
