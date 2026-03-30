package com.ridehailing.core_api.common.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

open class Ride {
  var id: UUID? = null
  var riderId: UUID? = null
  var driverId: UUID? = null
  var pickupLat: Double? = null
  var pickupLng: Double? = null
  var dropoffLat: Double? = null
  var dropoffLng: Double? = null

  // DB stores integer type ID; resolved via enum fromId()
  var statusTypeId: Int? = null

  var estimatedFare: BigDecimal? = null
  var finalFare: BigDecimal? = null
  var startedAt: Instant? = null
  var completedAt: Instant? = null
  var cancelledAt: Instant? = null
  var createdAt: Instant? = null
  var updatedAt: Instant? = null
  var searchRadiusKm: Double? = 5.0
  var surgeMultiplier: Double? = 1.0

  val status: RideStatus? get() = RideStatus.fromId(statusTypeId)

  fun setStatus(s: RideStatus) { statusTypeId = s.id }
}
