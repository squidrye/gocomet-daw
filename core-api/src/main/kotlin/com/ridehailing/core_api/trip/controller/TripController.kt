package com.ridehailing.core_api.trip

import com.ridehailing.core_api.trip.dto.TripResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/trips")
@Tag(name = "Trips")
open class TripController {

  @Autowired
  private lateinit var tripService: TripService

  @Operation(summary = "Start a trip")
  @PostMapping("/{rideId}/start")
  fun startTrip(@PathVariable rideId: UUID): ResponseEntity<TripResponse> {
    val driverId = getAuthUserId()
    val response = tripService.startTrip(rideId, driverId)
    return ResponseEntity.ok(response)
  }

  @Operation(summary = "End a trip and calculate fare")
  @PostMapping("/{rideId}/end")
  fun endTrip(@PathVariable rideId: UUID): ResponseEntity<TripResponse> {
    val driverId = getAuthUserId()
    val response = tripService.endTrip(rideId, driverId)
    return ResponseEntity.ok(response)
  }

  private fun getAuthUserId(): UUID {
    return SecurityContextHolder.getContext().authentication.principal as UUID
  }
}
