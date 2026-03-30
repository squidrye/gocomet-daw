package com.ridehailing.core_api.driver

import com.ridehailing.core_api.auth.AuthMapper
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import com.ridehailing.core_api.common.model.DriverLocation
import com.ridehailing.core_api.common.model.DriverStatus
import com.ridehailing.core_api.common.model.Ride
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.common.model.User
import com.ridehailing.core_api.driver.dto.LocationUpdateRequest
import com.ridehailing.core_api.driver.dto.StatusUpdateRequest
import com.ridehailing.core_api.ride.MatchingService
import com.ridehailing.core_api.ride.RideMapper
import com.ridehailing.core_api.ride.RideStateMachine
import com.ridehailing.core_api.sse.SSEService
import com.ridehailing.core_api.sse.dto.RideUpdateEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
open class DriverService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var driverLocationMapper: DriverLocationMapper

  @Autowired
  private lateinit var authMapper: AuthMapper

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var matchingService: MatchingService

  @Autowired
  private lateinit var sseService: SSEService

  /** Store driver's current location */
  fun updateLocation(driverId: UUID, request: LocationUpdateRequest): DriverLocation {
    log.info("updateLocation - driverId=$driverId")

    val errors = mutableListOf<String>()
    if (request.latitude == null || request.longitude == null) errors.add("latitude and longitude are required")
    else {
      if (request.latitude!! < -90 || request.latitude!! > 90) errors.add("latitude must be between -90 and 90")
      if (request.longitude!! < -180 || request.longitude!! > 180) errors.add("longitude must be between -180 and 180")
    }
    if (errors.isNotEmpty()) throw AppException(AppExceptionTypes.VALIDATION_FAILED, errors)

    val location = DriverLocation().apply {
      this.driverId = driverId
      this.latitude = request.latitude
      this.longitude = request.longitude
    }
    driverLocationMapper.insert(location)
    log.debug("updateLocation - stored location id=${location.id}")
    return location
  }

  /** Toggle driver online/offline status */
  fun updateStatus(driverId: UUID, request: StatusUpdateRequest): User {
    log.info("updateStatus - driverId=$driverId, targetStatus=${request.status}")

    if (request.status == null) throw AppException(AppExceptionTypes.VALIDATION_FAILED, listOf("status is required"))

    val user = getDriverById(driverId)
    val current = user.driverStatus ?: throw AppException(AppExceptionTypes.NOT_A_DRIVER)

    if (!DriverStateMachine.canTransition(current, request.status!!)) {
      throw AppException(AppExceptionTypes.DRIVER_INVALID_TRANSITION, current, request.status)
    }

    user.setDriverStatus(request.status)
    authMapper.updateDriverStatus(user)
    log.info("updateStatus - driver $driverId transitioned $current -> ${request.status}")
    return user
  }

  /** Get driver user by ID */
  fun getDriverById(driverId: UUID): User {
    return authMapper.getById(driverId) ?: throw AppException(AppExceptionTypes.DRIVER_NOT_FOUND)
  }

  /** Get latest location for a driver */
  fun getLatestLocation(driverId: UUID): DriverLocation? {
    return driverLocationMapper.getLatestByDriverId(driverId)
  }

  /** Get pending ride offer for this driver */
  fun getOffer(driverId: UUID): Ride? {
    log.info("getOffer - driverId=$driverId")
    return rideMapper.getMatchedRideForDriver(driverId)
  }

  /** Accept a pending ride offer */
  @Transactional
  fun acceptOffer(driverId: UUID): Ride {
    log.info("acceptOffer - driverId=$driverId")
    val driver = getDriverById(driverId)

    if (driver.driverStatus != DriverStatus.LOCKED) throw AppException(AppExceptionTypes.DRIVER_NOT_LOCKED)

    val ride = rideMapper.getMatchedRideForDriver(driverId) ?: throw AppException(AppExceptionTypes.NO_PENDING_OFFER)

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.ACCEPTED)) {
      throw AppException(AppExceptionTypes.RIDE_INVALID_TRANSITION, ride.status, RideStatus.ACCEPTED)
    }

    ride.setStatus(RideStatus.ACCEPTED)
    rideMapper.updateStatus(ride)

    driver.setDriverStatus(DriverStatus.ON_TRIP)
    authMapper.updateDriverStatus(driver)

    log.info("acceptOffer - driver $driverId accepted ride ${ride.id}")
    sseService.send(ride.id!!, RideUpdateEvent().apply {
      rideId = ride.id; status = ride.status; this.driverId = driverId
    })
    return ride
  }

  /** Decline a pending ride offer and attempt re-matching */
  @Transactional
  fun declineOffer(driverId: UUID): Ride {
    log.info("declineOffer - driverId=$driverId")
    val driver = getDriverById(driverId)

    if (driver.driverStatus != DriverStatus.LOCKED) throw AppException(AppExceptionTypes.DRIVER_NOT_LOCKED)

    val ride = rideMapper.getMatchedRideForDriver(driverId) ?: throw AppException(AppExceptionTypes.NO_PENDING_OFFER)

    driver.setDriverStatus(DriverStatus.AVAILABLE)
    authMapper.updateDriverStatus(driver)

    val nextDriver = matchingService.findNearestDriver(ride.pickupLat!!, ride.pickupLng!!, listOf(driverId))
    if (nextDriver != null) {
      ride.driverId = nextDriver.driverId
      ride.setStatus(RideStatus.MATCHED)
      rideMapper.updateDriver(ride)

      authMapper.getById(nextDriver.driverId!!)?.let { next ->
        next.setDriverStatus(DriverStatus.LOCKED)
        authMapper.updateDriverStatus(next)
      }
      log.info("declineOffer - re-matched ride ${ride.id} to driver ${nextDriver.driverId}")
    } else {
      ride.driverId = null
      ride.setStatus(RideStatus.REQUESTED)
      rideMapper.updateDriver(ride)
      log.info("declineOffer - no other driver available, ride ${ride.id} back to REQUESTED")
    }
    return ride
  }
}
