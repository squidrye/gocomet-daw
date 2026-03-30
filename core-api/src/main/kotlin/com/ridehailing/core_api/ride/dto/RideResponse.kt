package com.ridehailing.core_api.ride.dto

import com.ridehailing.core_api.common.model.RideStatus
import java.math.BigDecimal
import java.util.UUID

open class RideResponse {
  var id: UUID? = null
  var status: RideStatus? = null
  var pickupLat: Double? = null
  var pickupLng: Double? = null
  var dropoffLat: Double? = null
  var dropoffLng: Double? = null
  var estimatedFare: BigDecimal? = null
  var finalFare: BigDecimal? = null
  var driverId: UUID? = null
  var message: String? = null
}
