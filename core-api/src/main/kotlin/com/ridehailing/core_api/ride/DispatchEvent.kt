package com.ridehailing.core_api.ride

import java.util.UUID

enum class DispatchEventType {
  NEW_RIDE,
  RIDE_REMOVED,
  RADIUS_EXPANDED,
  DRIVER_CONNECTED,
  DRIVER_DECLINED
}

open class DispatchEvent {
  var type: DispatchEventType? = null
  var rideId: UUID? = null
  var pickupLat: Double? = null
  var pickupLng: Double? = null
  var searchRadiusKm: Double? = null
  var driverId: UUID? = null
}
