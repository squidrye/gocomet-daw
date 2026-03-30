package com.ridehailing.core_api.ride

import com.ridehailing.core_api.ride.dto.CreateRideRequest
import com.ridehailing.core_api.ride.dto.RideResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/rides")
open class RideController {

  @Autowired
  private lateinit var rideService: RideService

  /** Create a new ride request */
  @PostMapping
  fun createRide(@RequestBody request: CreateRideRequest): ResponseEntity<RideResponse> {
    val riderId = getAuthUserId()
    val response = rideService.createRide(riderId, request)
    return ResponseEntity.status(HttpStatus.CREATED).body(response)
  }

  /** Get ride details */
  @GetMapping("/{id}")
  fun getRide(@PathVariable id: UUID): ResponseEntity<RideResponse> {
    val userId = getAuthUserId()
    val response = rideService.getRide(id, userId)
    return ResponseEntity.ok(response)
  }

  /** Cancel a ride */
  @PostMapping("/{id}/cancel")
  fun cancelRide(@PathVariable id: UUID): ResponseEntity<RideResponse> {
    val riderId = getAuthUserId()
    val response = rideService.cancelRide(id, riderId)
    return ResponseEntity.ok(response)
  }

  private fun getAuthUserId(): UUID {
    return SecurityContextHolder.getContext().authentication.principal as UUID
  }
}
