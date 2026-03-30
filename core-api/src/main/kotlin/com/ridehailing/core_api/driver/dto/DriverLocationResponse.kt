package com.ridehailing.core_api.driver.dto

import java.time.Instant
import java.util.UUID

open class DriverLocationResponse {
  var driverId: UUID? = null
  var latitude: Double? = null
  var longitude: Double? = null
  var timestamp: Instant? = null
}
