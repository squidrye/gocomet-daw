package com.ridehailing.core_api.common.model

import java.time.Instant
import java.util.UUID

open class DriverLocation {
  var id: UUID? = null
  var driverId: UUID? = null
  var latitude: Double? = null
  var longitude: Double? = null
  var createdAt: Instant? = null
}
