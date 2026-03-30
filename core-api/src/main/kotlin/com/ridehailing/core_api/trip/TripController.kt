package com.ridehailing.core_api.trip

import com.ridehailing.core_api.trip.dto.TripResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/trips")
open class TripController {

  @Autowired
  private lateinit var tripService: TripService

  /** Start a trip */
  @PostMapping("/{rideId}/start")
  fun startTrip(@PathVariable rideId: UUID): ResponseEntity<TripResponse> {
    val driverId = getAuthUserId()
    val response = tripService.startTrip(rideId, driverId)
    return ResponseEntity.ok(response)
  }

  /** End a trip */
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
