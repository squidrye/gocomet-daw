package com.ridehailing.core_api.trip

import com.ridehailing.core_api.auth.AuthMapper
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import com.ridehailing.core_api.common.model.DriverStatus
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.driver.RedisDriverLocationService
import com.ridehailing.core_api.ride.RideDispatchService
import com.ridehailing.core_api.ride.RideMapper
import com.ridehailing.core_api.ride.RideStateMachine
import com.ridehailing.core_api.sse.SSEService
import com.ridehailing.core_api.sse.dto.RideUpdateEvent
import com.ridehailing.core_api.trip.dto.TripResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

  @Autowired
  private lateinit var rideDispatchService: RideDispatchService

  @Autowired
  private lateinit var redisLocationService: RedisDriverLocationService

  /** Start a trip — ACCEPTED → IN_PROGRESS */
  fun startTrip(rideId: UUID, driverId: UUID): TripResponse {
    log.info("startTrip - rideId=$rideId, driverId=$driverId")

    val ride = rideMapper.getById(rideId) ?: throw AppException(AppExceptionTypes.RIDE_NOT_FOUND)
    if (ride.driverId != driverId) throw AppException(AppExceptionTypes.TRIP_DRIVER_MISMATCH)

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.IN_PROGRESS)) {
      throw AppException(AppExceptionTypes.TRIP_INVALID_TRANSITION, ride.status)
    }

    ride.setStatus(RideStatus.IN_PROGRESS)
    ride.startedAt = Instant.now()
    rideMapper.updateTripTimes(ride)
    log.info("startTrip - ride $rideId now IN_PROGRESS")

    sseService.send(rideId, RideUpdateEvent().apply {
      this.rideId = ride.id; status = ride.status
    })
    return TripResponse().apply { this.rideId = ride.id; this.status = ride.status }
  }

  /** End a trip — IN_PROGRESS → COMPLETED, calculate fare, release driver */
  @Transactional
  fun endTrip(rideId: UUID, driverId: UUID): TripResponse {
    log.info("endTrip - rideId=$rideId, driverId=$driverId")

    val ride = rideMapper.getById(rideId) ?: throw AppException(AppExceptionTypes.RIDE_NOT_FOUND)
    if (ride.driverId != driverId) throw AppException(AppExceptionTypes.TRIP_DRIVER_MISMATCH)

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.COMPLETED)) {
      throw AppException(AppExceptionTypes.TRIP_INVALID_TRANSITION, ride.status)
    }

    val finalFare = fareCalculator.calculate(
      ride.pickupLat!!, ride.pickupLng!!,
      ride.dropoffLat!!, ride.dropoffLng!!
    )

    ride.setStatus(RideStatus.COMPLETED)
    ride.finalFare = finalFare
    ride.completedAt = Instant.now()
    rideMapper.updateTripTimes(ride)

    authMapper.getById(driverId)?.let { driver ->
      driver.setDriverStatus(DriverStatus.AVAILABLE)
      authMapper.updateDriverStatus(driver)
      redisLocationService.markAvailable(driverId)
      log.info("endTrip - released driver $driverId back to AVAILABLE")
    }

    log.info("endTrip - ride $rideId COMPLETED, finalFare=$finalFare")
    sseService.send(rideId, RideUpdateEvent().apply {
      this.rideId = ride.id; status = ride.status; this.finalFare = finalFare
    })
    return TripResponse().apply { this.rideId = ride.id; this.status = ride.status; this.finalFare = finalFare }
  }
}
