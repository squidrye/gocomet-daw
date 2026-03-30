package com.ridehailing.core_api.common.model

import java.time.Instant
import java.util.UUID

open class RideDecline {
  var id: UUID? = null
  var rideId: UUID? = null
  var driverId: UUID? = null
  var createdAt: Instant? = null
  var hash: String? = null
}
