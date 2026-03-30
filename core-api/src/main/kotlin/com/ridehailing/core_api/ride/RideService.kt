package com.ridehailing.core_api.ride

import com.ridehailing.core_api.auth.AuthMapper
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.model.DriverStatus
import com.ridehailing.core_api.common.model.Ride
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.ride.dto.CreateRideRequest
import com.ridehailing.core_api.ride.dto.RideResponse
import com.ridehailing.core_api.sse.SSEService
import com.ridehailing.core_api.trip.FareCalculator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
open class RideService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var matchingService: MatchingService

  @Autowired
  private lateinit var fareCalculator: FareCalculator

  @Autowired
  private lateinit var authMapper: AuthMapper

  @Autowired
  private lateinit var sseService: SSEService

  /** Create a new ride request and attempt matching */
  fun createRide(riderId: UUID, request: CreateRideRequest): RideResponse {
    log.info("createRide - riderId=$riderId")
    validateCoordinates(request)

    val estimatedFare = fareCalculator.calculate(
      request.pickupLat!!, request.pickupLng!!,
      request.dropoffLat!!, request.dropoffLng!!
    )

    val ride = Ride().apply {
      this.riderId = riderId
      this.pickupLat = request.pickupLat
      this.pickupLng = request.pickupLng
      this.dropoffLat = request.dropoffLat
      this.dropoffLng = request.dropoffLng
      this.status = RideStatus.REQUESTED
      this.estimatedFare = estimatedFare
    }
    rideMapper.insert(ride)
    log.info("createRide - ride created id=${ride.id}")

    val nearestDriver = matchingService.findNearestDriver(request.pickupLat!!, request.pickupLng!!)
    if (nearestDriver != null) {
      ride.apply {
        this.driverId = nearestDriver.driverId
        this.status = RideStatus.MATCHED
      }
      rideMapper.updateDriver(ride)

      val driverUser = authMapper.getById(nearestDriver.driverId!!)
      if (driverUser != null) {
        driverUser.apply { driverStatus = DriverStatus.LOCKED }
        authMapper.updateDriverStatus(driverUser)
      }
      log.info("createRide - matched with driverId=${nearestDriver.driverId}")

      sseService.send(ride.id!!, mapOf(
        "rideId" to ride.id,
        "status" to ride.status,
        "driverId" to ride.driverId
      ))

      return toResponse(ride)
    }

    log.info("createRide - no drivers available nearby")
    return toResponse(ride).apply {
      message = "No drivers available nearby"
    }
  }

  /** Get ride details with access control */
  fun getRide(rideId: UUID, userId: UUID): RideResponse {
    log.info("getRide - rideId=$rideId, userId=$userId")
    val ride = rideMapper.getById(rideId)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "Ride not found")

    if (ride.riderId != userId && ride.driverId != userId) {
      throw AppException(status = HttpStatus.FORBIDDEN, message = "Access denied")
    }
    return toResponse(ride)
  }

  /** Cancel a ride */
  fun cancelRide(rideId: UUID, riderId: UUID): RideResponse {
    log.info("cancelRide - rideId=$rideId, riderId=$riderId")
    val ride = rideMapper.getById(rideId)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "Ride not found")

    if (ride.riderId != riderId) {
      throw AppException(status = HttpStatus.FORBIDDEN, message = "Only the rider can cancel this ride")
    }

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.CANCELLED)) {
      throw AppException(
        status = HttpStatus.CONFLICT,
        message = "Cannot cancel ride in ${ride.status} state"
      )
    }

    // Release driver if one was assigned
    if (ride.driverId != null && (ride.status == RideStatus.MATCHED || ride.status == RideStatus.ACCEPTED)) {
      val driverUser = authMapper.getById(ride.driverId!!)
      if (driverUser != null) {
        driverUser.apply { driverStatus = DriverStatus.AVAILABLE }
        authMapper.updateDriverStatus(driverUser)
        log.info("cancelRide - released driver ${ride.driverId} back to AVAILABLE")
      }
    }

    ride.apply {
      status = RideStatus.CANCELLED
      cancelledAt = Instant.now()
    }
    rideMapper.updateStatus(ride)
    log.info("cancelRide - ride $rideId cancelled")

    sseService.send(rideId, mapOf(
      "rideId" to ride.id,
      "status" to ride.status
    ))

    return toResponse(ride)
  }

  /** Map Ride entity to RideResponse */
  fun toResponse(ride: Ride): RideResponse {
    return RideResponse().apply {
      id = ride.id
      status = ride.status
      pickupLat = ride.pickupLat
      pickupLng = ride.pickupLng
      dropoffLat = ride.dropoffLat
      dropoffLng = ride.dropoffLng
      estimatedFare = ride.estimatedFare
      finalFare = ride.finalFare
      driverId = ride.driverId
    }
  }

  private fun validateCoordinates(request: CreateRideRequest) {
    val errors = mutableListOf<String>()
    if (request.pickupLat == null) errors.add("pickupLat is required")
    if (request.pickupLng == null) errors.add("pickupLng is required")
    if (request.dropoffLat == null) errors.add("dropoffLat is required")
    if (request.dropoffLng == null) errors.add("dropoffLng is required")
    if (errors.isNotEmpty()) {
      throw AppException(status = HttpStatus.BAD_REQUEST, message = "Validation failed", details = errors)
    }

    if (request.pickupLat!! < -90 || request.pickupLat!! > 90) errors.add("pickupLat must be between -90 and 90")
    if (request.pickupLng!! < -180 || request.pickupLng!! > 180) errors.add("pickupLng must be between -180 and 180")
    if (request.dropoffLat!! < -90 || request.dropoffLat!! > 90) errors.add("dropoffLat must be between -90 and 90")
    if (request.dropoffLng!! < -180 || request.dropoffLng!! > 180) errors.add("dropoffLng must be between -180 and 180")
    if (errors.isNotEmpty()) {
      throw AppException(status = HttpStatus.BAD_REQUEST, message = "Validation failed", details = errors)
    }
  }
}
