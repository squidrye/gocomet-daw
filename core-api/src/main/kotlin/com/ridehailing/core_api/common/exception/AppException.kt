package com.ridehailing.core_api.common.exception

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.http.HttpStatus

@JsonIgnoreProperties("cause", "stackTrace", "suppressed", "localizedMessage")
open class AppException(
  var code: Int = 0,
  var status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
  override var message: String = "Internal server error",
  var details: List<String>? = null
) : RuntimeException(message)
