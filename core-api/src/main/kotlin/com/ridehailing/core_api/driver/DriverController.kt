package com.ridehailing.core_api.driver

import com.ridehailing.core_api.common.model.DriverLocation
import com.ridehailing.core_api.driver.dto.DriverStatusResponse
import com.ridehailing.core_api.driver.dto.LocationUpdateRequest
import com.ridehailing.core_api.driver.dto.RideActionResponse
import com.ridehailing.core_api.driver.dto.StatusUpdateRequest
import com.ridehailing.core_api.ride.RideDispatchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/drivers/me")
open class DriverController {

  @Autowired
  private lateinit var driverService: DriverService

  @Autowired
  private lateinit var rideDispatchService: RideDispatchService

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

  /** SSE stream for available rides near this driver */
  @GetMapping("/rides/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun streamAvailableRides(): SseEmitter {
    val driverId = getAuthUserId()
    val emitter = rideDispatchService.registerDriver(driverId)
    rideDispatchService.notifyDriverOnConnect(driverId)
    return emitter
  }

  /** Accept a ride */
  @PostMapping("/rides/{rideId}/accept")
  fun acceptRide(@PathVariable rideId: UUID): ResponseEntity<RideActionResponse> {
    val driverId = getAuthUserId()
    val ride = driverService.acceptRide(driverId, rideId)
    val response = RideActionResponse().apply {
      this.rideId = ride.id
      status = ride.status
    }
    return ResponseEntity.ok(response)
  }

  /** Decline a ride */
  @PostMapping("/rides/{rideId}/decline")
  fun declineRide(@PathVariable rideId: UUID): ResponseEntity<Void> {
    val driverId = getAuthUserId()
    driverService.declineRide(driverId, rideId)
    return ResponseEntity.noContent().build()
  }

  private fun getAuthUserId(): UUID {
    return SecurityContextHolder.getContext().authentication.principal as UUID
  }
}
