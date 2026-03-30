package com.ridehailing.core_api.trip

import com.ridehailing.core_api.auth.AuthMapper
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.model.DriverStatus
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.ride.RideMapper
import com.ridehailing.core_api.ride.RideStateMachine
import com.ridehailing.core_api.sse.SSEService
import com.ridehailing.core_api.trip.dto.TripResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
open class TripService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var fareCalculator: FareCalculator

  @Autowired
  private lateinit var authMapper: AuthMapper

  @Autowired
  private lateinit var sseService: SSEService

  /** Start a trip — ACCEPTED → IN_PROGRESS */
  fun startTrip(rideId: UUID, driverId: UUID): TripResponse {
    log.info("startTrip - rideId=$rideId, driverId=$driverId")

    val ride = rideMapper.getById(rideId)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "Ride not found")

    if (ride.driverId != driverId) {
      throw AppException(status = HttpStatus.FORBIDDEN, message = "Driver not assigned to this ride")
    }

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.IN_PROGRESS)) {
      throw AppException(
        status = HttpStatus.CONFLICT,
        message = "Cannot start trip in ${ride.status} state"
      )
    }

    ride.apply {
      status = RideStatus.IN_PROGRESS
      startedAt = Instant.now()
    }
    rideMapper.updateTripTimes(ride)
    log.info("startTrip - ride $rideId now IN_PROGRESS")

    sseService.send(rideId, mapOf(
      "rideId" to ride.id,
      "status" to ride.status
    ))

    return TripResponse().apply {
      this.rideId = ride.id
      this.status = ride.status
    }
  }

  /** End a trip — IN_PROGRESS → COMPLETED, calculate fare, release driver */
  fun endTrip(rideId: UUID, driverId: UUID): TripResponse {
    log.info("endTrip - rideId=$rideId, driverId=$driverId")

    val ride = rideMapper.getById(rideId)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "Ride not found")

    if (ride.driverId != driverId) {
      throw AppException(status = HttpStatus.FORBIDDEN, message = "Driver not assigned to this ride")
    }

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.COMPLETED)) {
      throw AppException(
        status = HttpStatus.CONFLICT,
        message = "Cannot end trip in ${ride.status} state"
      )
    }

    val finalFare = fareCalculator.calculate(
      ride.pickupLat!!, ride.pickupLng!!,
      ride.dropoffLat!!, ride.dropoffLng!!
    )

    ride.apply {
      status = RideStatus.COMPLETED
      this.finalFare = finalFare
      completedAt = Instant.now()
    }
    rideMapper.updateTripTimes(ride)

    // Release driver back to AVAILABLE
    val driver = authMapper.getById(driverId)
    if (driver != null) {
      driver.apply { driverStatus = DriverStatus.AVAILABLE }
      authMapper.updateDriverStatus(driver)
      log.info("endTrip - released driver $driverId back to AVAILABLE")
    }

    log.info("endTrip - ride $rideId COMPLETED, finalFare=$finalFare")

    sseService.send(rideId, mapOf(
      "rideId" to ride.id,
      "status" to ride.status,
      "finalFare" to finalFare
    ))

    return TripResponse().apply {
      this.rideId = ride.id
      this.status = ride.status
      this.finalFare = finalFare
    }
  }
}
