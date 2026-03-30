package com.ridehailing.core_api.ride.dto

import java.math.BigDecimal
import java.util.UUID

open class AvailableRideResponse {
  var rideId: UUID? = null
  var pickupLat: Double? = null
  var pickupLng: Double? = null
  var dropoffLat: Double? = null
  var dropoffLng: Double? = null
  var estimatedFare: BigDecimal? = null
  var surgeMultiplier: Double? = null
}
