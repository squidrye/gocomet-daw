package com.ridehailing.core_api.config

import com.ridehailing.core_api.common.util.InputSanitizer
import org.springframework.core.MethodParameter
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter
import java.lang.reflect.Type
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

@ControllerAdvice
open class SanitizationAdvice : RequestBodyAdviceAdapter() {

  override fun supports(
    methodParameter: MethodParameter,
    targetType: Type,
    converterType: Class<out HttpMessageConverter<*>>
  ): Boolean = true

  override fun afterBodyRead(
    body: Any,
    inputMessage: HttpInputMessage,
    parameter: MethodParameter,
    targetType: Type,
    converterType: Class<out HttpMessageConverter<*>>
  ): Any {
    sanitizeObject(body)
    return body
  }

  private fun sanitizeObject(obj: Any) {
    obj::class.memberProperties.forEach { prop ->
      if (prop is KMutableProperty1<*, *> && prop.returnType.classifier == String::class) {
        try {
          @Suppress("UNCHECKED_CAST")
          val mutableProp = prop as KMutableProperty1<Any, String?>
          val value = mutableProp.get(obj)
          if (value != null) {
            mutableProp.set(obj, InputSanitizer.sanitize(value))
          }
        } catch (_: Exception) {}
      }
    }
  }
}
