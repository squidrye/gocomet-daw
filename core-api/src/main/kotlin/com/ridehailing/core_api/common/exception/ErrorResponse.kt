package com.ridehailing.core_api.common.exception

open class ErrorResponse {
  var error: String? = null
  var details: List<String>? = null
}
