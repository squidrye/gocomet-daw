package com.ridehailing.core_api.driver

import com.ridehailing.core_api.auth.AuthMapper
import com.ridehailing.core_api.common.exception.AppException
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
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

    if (request.latitude == null || request.longitude == null) {
      throw AppException(
        status = HttpStatus.BAD_REQUEST,
        message = "Validation failed",
        details = listOf("latitude and longitude are required")
      )
    }
    if (request.latitude!! < -90 || request.latitude!! > 90) {
      throw AppException(
        status = HttpStatus.BAD_REQUEST,
        message = "Validation failed",
        details = listOf("latitude must be between -90 and 90")
      )
    }
    if (request.longitude!! < -180 || request.longitude!! > 180) {
      throw AppException(
        status = HttpStatus.BAD_REQUEST,
        message = "Validation failed",
        details = listOf("longitude must be between -180 and 180")
      )
    }

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

    if (request.status == null) {
      throw AppException(status = HttpStatus.BAD_REQUEST, message = "status is required")
    }

    val currentUser = getDriverById(driverId)
    val currentStatus = currentUser.driverStatus
      ?: throw AppException(status = HttpStatus.CONFLICT, message = "User is not a driver")

    if (!DriverStateMachine.canTransition(currentStatus, request.status!!)) {
      throw AppException(
        status = HttpStatus.CONFLICT,
        message = "Invalid state transition from $currentStatus to ${request.status}"
      )
    }

    currentUser.apply {
      driverStatus = request.status
    }
    authMapper.updateDriverStatus(currentUser)
    log.info("updateStatus - driver $driverId transitioned $currentStatus -> ${request.status}")
    return currentUser
  }

  /** Get driver user by ID */
  fun getDriverById(driverId: UUID): User {
    return authMapper.getById(driverId)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "Driver not found")
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
  fun acceptOffer(driverId: UUID): Ride {
    log.info("acceptOffer - driverId=$driverId")
    val driver = getDriverById(driverId)

    if (driver.driverStatus != DriverStatus.LOCKED) {
      throw AppException(status = HttpStatus.CONFLICT, message = "Driver is not in LOCKED state")
    }

    val ride = rideMapper.getMatchedRideForDriver(driverId)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "No pending offer found")

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.ACCEPTED)) {
      throw AppException(status = HttpStatus.CONFLICT, message = "Ride cannot be accepted in ${ride.status} state")
    }

    ride.apply { status = RideStatus.ACCEPTED }
    rideMapper.updateStatus(ride)

    driver.apply { driverStatus = DriverStatus.ON_TRIP }
    authMapper.updateDriverStatus(driver)

    log.info("acceptOffer - driver $driverId accepted ride ${ride.id}")

    sseService.send(ride.id!!, mapOf(
      "rideId" to ride.id,
      "status" to ride.status,
      "driverId" to driverId
    ))

    return ride
  }

  /** Decline a pending ride offer and attempt re-matching */
  fun declineOffer(driverId: UUID): Ride {
    log.info("declineOffer - driverId=$driverId")
    val driver = getDriverById(driverId)

    if (driver.driverStatus != DriverStatus.LOCKED) {
      throw AppException(status = HttpStatus.CONFLICT, message = "Driver is not in LOCKED state")
    }

    val ride = rideMapper.getMatchedRideForDriver(driverId)
      ?: throw AppException(status = HttpStatus.NOT_FOUND, message = "No pending offer found")

    // Release this driver back to AVAILABLE
    driver.apply { driverStatus = DriverStatus.AVAILABLE }
    authMapper.updateDriverStatus(driver)

    // Try to find another driver, excluding the one who declined
    val excludeIds = listOf(driverId)
    val nextDriver = matchingService.findNearestDriver(ride.pickupLat!!, ride.pickupLng!!, excludeIds)

    if (nextDriver != null) {
      ride.apply {
        this.driverId = nextDriver.driverId
        this.status = RideStatus.MATCHED
      }
      rideMapper.updateDriver(ride)

      val nextDriverUser = authMapper.getById(nextDriver.driverId!!)
      if (nextDriverUser != null) {
        nextDriverUser.apply { driverStatus = DriverStatus.LOCKED }
        authMapper.updateDriverStatus(nextDriverUser)
      }
      log.info("declineOffer - re-matched ride ${ride.id} to driver ${nextDriver.driverId}")
    } else {
      ride.apply {
        this.driverId = null
        this.status = RideStatus.REQUESTED
      }
      rideMapper.updateDriver(ride)
      log.info("declineOffer - no other driver available, ride ${ride.id} back to REQUESTED")
    }

    return ride
  }
}
