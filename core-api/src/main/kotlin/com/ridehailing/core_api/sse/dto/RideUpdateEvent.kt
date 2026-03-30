package com.ridehailing.core_api.sse.dto

import com.ridehailing.core_api.common.model.RideStatus
import java.math.BigDecimal
import java.util.UUID

open class RideUpdateEvent {
  var rideId: UUID? = null
  var status: RideStatus? = null
  var driverId: UUID? = null
  var finalFare: BigDecimal? = null
}
