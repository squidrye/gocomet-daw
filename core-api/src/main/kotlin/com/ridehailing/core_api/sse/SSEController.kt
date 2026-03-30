package com.ridehailing.core_api.sse

import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.driver.DriverService
import com.ridehailing.core_api.ride.RideMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/rides")
open class SSEController {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var sseService: SSEService

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var driverService: DriverService

  /** SSE stream for ride updates */
  @GetMapping("/{id}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun streamEvents(@PathVariable id: UUID): SseEmitter {
    log.info("streamEvents - rideId=$id")
    return sseService.register(id)
  }

  /** Get driver's current location for rider polling */
  @GetMapping("/{id}/driver-location")
  fun getDriverLocation(@PathVariable id: UUID): ResponseEntity<Map<String, Any?>> {
    log.info("getDriverLocation - rideId=$id")
    val riderId = getAuthUserId()

    val ride = rideMapper.getById(id)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "Ride not found")

    if (ride.riderId != riderId) {
      throw AppException(status = HttpStatus.FORBIDDEN, message = "Access denied")
    }

    if (ride.status != RideStatus.ACCEPTED && ride.status != RideStatus.IN_PROGRESS) {
      throw AppException(
        status = HttpStatus.FORBIDDEN,
        message = "Driver location only available for ACCEPTED or IN_PROGRESS rides"
      )
    }

    val location = driverService.getLatestLocation(ride.driverId!!)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "Driver location not available")

    return ResponseEntity.ok(mapOf(
      "driverId" to ride.driverId,
      "latitude" to location.latitude,
      "longitude" to location.longitude,
      "timestamp" to location.createdAt
    ))
  }

  private fun getAuthUserId(): UUID {
    return SecurityContextHolder.getContext().authentication.principal as UUID
  }
}
