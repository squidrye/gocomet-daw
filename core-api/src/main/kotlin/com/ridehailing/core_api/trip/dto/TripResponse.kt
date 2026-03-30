package com.ridehailing.core_api.trip.dto

import com.ridehailing.core_api.common.model.RideStatus
import java.math.BigDecimal
import java.util.UUID

open class TripResponse {
  var rideId: UUID? = null
  var status: RideStatus? = null
  var finalFare: BigDecimal? = null
}
