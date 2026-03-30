package com.ridehailing.core_api.driver

import com.ridehailing.core_api.common.model.DriverLocation
import com.ridehailing.core_api.driver.dto.DriverStatusResponse
import com.ridehailing.core_api.driver.dto.LocationUpdateRequest
import com.ridehailing.core_api.driver.dto.OfferResponse
import com.ridehailing.core_api.driver.dto.RideActionResponse
import com.ridehailing.core_api.driver.dto.StatusUpdateRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/drivers/me")
open class DriverController {

  @Autowired
  private lateinit var driverService: DriverService

  /** Update driver's current location */
  @PutMapping("/location")
  fun updateLocation(@RequestBody request: LocationUpdateRequest): ResponseEntity<DriverLocation> {
    val driverId = getAuthUserId()
    val location = driverService.updateLocation(driverId, request)
    return ResponseEntity.ok(location)
  }

  /** Toggle driver online/offline */
  @PutMapping("/status")
  fun updateStatus(@RequestBody request: StatusUpdateRequest): ResponseEntity<DriverStatusResponse> {
    val driverId = getAuthUserId()
    val user = driverService.updateStatus(driverId, request)
    val response = DriverStatusResponse().apply {
      userId = user.id
      driverStatus = user.driverStatus
    }
    return ResponseEntity.ok(response)
  }

  /** Poll for pending ride offer */
  @GetMapping("/offer")
  fun getOffer(): ResponseEntity<OfferResponse> {
    val driverId = getAuthUserId()
    val ride = driverService.getOffer(driverId)
      ?: return ResponseEntity.noContent().build()
    val response = OfferResponse().apply {
      rideId = ride.id
      status = ride.status
      pickupLat = ride.pickupLat
      pickupLng = ride.pickupLng
      dropoffLat = ride.dropoffLat
      dropoffLng = ride.dropoffLng
      estimatedFare = ride.estimatedFare
    }
    return ResponseEntity.ok(response)
  }

  /** Accept pending ride offer */
  @PostMapping("/offer/accept")
  fun acceptOffer(): ResponseEntity<RideActionResponse> {
    val driverId = getAuthUserId()
    val ride = driverService.acceptOffer(driverId)
    val response = RideActionResponse().apply {
      rideId = ride.id
      status = ride.status
    }
    return ResponseEntity.ok(response)
  }

  /** Decline pending ride offer */
  @PostMapping("/offer/decline")
  fun declineOffer(): ResponseEntity<RideActionResponse> {
    val driverId = getAuthUserId()
    val ride = driverService.declineOffer(driverId)
    val response = RideActionResponse().apply {
      rideId = ride.id
      status = ride.status
    }
    return ResponseEntity.ok(response)
  }

  private fun getAuthUserId(): UUID {
    return SecurityContextHolder.getContext().authentication.principal as UUID
  }
}
