package com.ridehailing.core_api.driver

import com.ridehailing.core_api.common.model.DriverLocation
import com.ridehailing.core_api.driver.dto.DriverStatusResponse
import com.ridehailing.core_api.driver.dto.LocationUpdateRequest
import com.ridehailing.core_api.driver.dto.RideActionResponse
import com.ridehailing.core_api.driver.dto.StatusUpdateRequest
import com.ridehailing.core_api.ride.RideDispatchService
import com.ridehailing.core_api.ride.RideMapper
import com.ridehailing.core_api.ride.RideService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/drivers/me")
@Tag(name = "Drivers")
open class DriverController {

  @Autowired
  private lateinit var driverService: DriverService

  @Autowired
  private lateinit var rideDispatchService: RideDispatchService

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var rideService: RideService

  @Operation(summary = "Update driver location")
  @PutMapping("/location")
  fun updateLocation(@RequestBody request: LocationUpdateRequest): ResponseEntity<DriverLocation> {
    val driverId = getAuthUserId()
    val location = driverService.updateLocation(driverId, request)
    return ResponseEntity.ok(location)
  }

  @Operation(summary = "Get current driver status and sync state")
  @GetMapping("/status")
  fun getStatus(): ResponseEntity<DriverStatusResponse> {
    val driverId = getAuthUserId()
    val user = driverService.syncDriverState(driverId)
    val response = DriverStatusResponse().apply {
      userId = user.id
      driverStatus = user.driverStatus
    }
    return ResponseEntity.ok(response)
  }

  @Operation(summary = "Go online or offline")
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

  @Operation(summary = "Get driver's active trip")
  @GetMapping("/active-ride")
  fun getActiveRide(): ResponseEntity<com.ridehailing.core_api.ride.dto.RideResponse> {
    val driverId = getAuthUserId()
    val ride = rideMapper.getActiveRideForDriver(driverId)
      ?: return ResponseEntity.noContent().build()
    return ResponseEntity.ok(rideService.toResponse(ride))
  }

  @Operation(summary = "SSE stream for nearby available rides")
  @GetMapping("/rides/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun streamAvailableRides(): SseEmitter {
    val driverId = getAuthUserId()
    val emitter = rideDispatchService.registerDriver(driverId)
    rideDispatchService.notifyDriverOnConnect(driverId)
    return emitter
  }

  @Operation(summary = "Accept a ride")
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

  @Operation(summary = "Decline a ride")
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
