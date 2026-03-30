package com.ridehailing.core_api.ride.dto

open class CreateRideRequest {
  var pickupLat: Double? = null
  var pickupLng: Double? = null
  var dropoffLat: Double? = null
  var dropoffLng: Double? = null
}
