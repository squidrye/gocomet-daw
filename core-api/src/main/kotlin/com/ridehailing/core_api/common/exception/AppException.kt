package com.ridehailing.core_api.common.exception

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.http.HttpStatus

@JsonIgnoreProperties("cause", "stackTrace", "suppressed", "localizedMessage")
open class AppException(
  var code: Int = 0,
  var status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
  override var message: String = "Internal server error",
  var details: List<String>? = null
) : RuntimeException(message) {

  /** Construct from a triplet defined in AppExceptionTypes */
  constructor(
    triplet: Triple<Int, HttpStatus, String>,
    details: List<String>? = null
  ) : this(triplet.first, triplet.second, triplet.third, details)

  /** Construct from a triplet with a formatted message */
  constructor(
    triplet: Triple<Int, HttpStatus, String>,
    vararg args: Any?
  ) : this(triplet.first, triplet.second, triplet.third.format(*args), null)
}
