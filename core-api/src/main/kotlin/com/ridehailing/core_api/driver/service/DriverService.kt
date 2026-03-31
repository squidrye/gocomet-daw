package com.ridehailing.core_api.driver

import com.ridehailing.core_api.auth.AuthMapper
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import com.ridehailing.core_api.common.util.IdempotencyHash
import com.ridehailing.core_api.common.model.DriverLocation
import com.ridehailing.core_api.common.model.DriverStatus
import com.ridehailing.core_api.common.model.RideDecline
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.common.model.User
import com.ridehailing.core_api.driver.dto.LocationUpdateRequest
import com.ridehailing.core_api.driver.dto.StatusUpdateRequest
import com.ridehailing.core_api.ride.RideDeclineMapper
import com.ridehailing.core_api.ride.RideDispatchService
import com.ridehailing.core_api.ride.RideMapper
import com.ridehailing.core_api.ride.RideQueueService
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

  companion object {
    const val REFRESH_DISTANCE_KM = 0.5
  }

  private val lastRefreshPosition = java.util.concurrent.ConcurrentHashMap<UUID, Pair<Double, Double>>()

  @Autowired
  private lateinit var driverLocationMapper: DriverLocationMapper

  @Autowired
  private lateinit var redisLocationService: RedisDriverLocationService

  @Autowired
  private lateinit var authMapper: AuthMapper

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var rideDeclineMapper: RideDeclineMapper

  @Autowired
  private lateinit var rideDispatchService: RideDispatchService

  @Autowired
  private lateinit var rideQueueService: RideQueueService

  @Autowired
  private lateinit var sseService: SSEService

  @Autowired
  private lateinit var locationAuditBuffer: LocationAuditBuffer

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

    // Redis: hot-path for fast geo lookups
    redisLocationService.updateLocation(driverId, request.latitude!!, request.longitude!!)

    locationAuditBuffer.add(location)
    log.debug("updateLocation - buffered for audit")

    // If driver is connected to dispatch SSE, refresh their available rides
    if (rideDispatchService.isDriverConnected(driverId)) {
      val lastPos = lastRefreshPosition[driverId]
      val shouldRefresh = lastPos == null ||
        com.ridehailing.core_api.common.util.HaversineUtil.distanceKm(
          lastPos.first, lastPos.second, request.latitude!!, request.longitude!!
        ) >= REFRESH_DISTANCE_KM

      if (shouldRefresh) {
        lastRefreshPosition[driverId] = Pair(request.latitude!!, request.longitude!!)
        rideDispatchService.notifyDriverMoved(driverId)
      }
    }

    val activeRideId = redisLocationService.getActiveRideId(driverId)
    if (activeRideId != null) {
      rideDispatchService.publishDriverLocation(activeRideId, request.latitude!!, request.longitude!!)
    }

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

    when (request.status) {
      DriverStatus.AVAILABLE -> redisLocationService.markAvailable(driverId)
      DriverStatus.OFFLINE -> {
        redisLocationService.markUnavailable(driverId)
        rideDispatchService.removeDriver(driverId)
      }
      else -> redisLocationService.markUnavailable(driverId)
    }

    return user
  }

  /** Get driver user by ID */
  fun getDriverById(driverId: UUID): User {
    return authMapper.getById(driverId) ?: throw AppException(AppExceptionTypes.DRIVER_NOT_FOUND)
  }

  fun syncDriverState(driverId: UUID): User {
    log.info("syncDriverState - driverId=$driverId")
    val user = getDriverById(driverId)

    val activeRide = rideMapper.getActiveRideForDriver(driverId)
    if (activeRide != null && user.driverStatus != DriverStatus.ON_TRIP) {
      user.setDriverStatus(DriverStatus.ON_TRIP)
      authMapper.updateDriverStatus(user)
      redisLocationService.markUnavailable(driverId)
      log.info("syncDriverState - driver $driverId has active ride, reset to ON_TRIP")
      return user
    }

    if (user.driverStatus == DriverStatus.AVAILABLE && !rideDispatchService.isDriverConnected(driverId)) {
      user.setDriverStatus(DriverStatus.OFFLINE)
      authMapper.updateDriverStatus(user)
      redisLocationService.markUnavailable(driverId)
      log.info("syncDriverState - driver $driverId was stale AVAILABLE, reset to OFFLINE")
    }

    return user
  }

  /** Get latest location for a driver — Redis first, PG fallback */
  fun getLatestLocation(driverId: UUID): DriverLocation? {
    val redisLoc = redisLocationService.getLocation(driverId)
    if (redisLoc != null) {
      return DriverLocation().apply {
        this.driverId = driverId
        this.latitude = redisLoc.first
        this.longitude = redisLoc.second
      }
    }
    return driverLocationMapper.getLatestByDriverId(driverId)
  }

  /** Accept a ride — REQUESTED → ACCEPTED, driver → ON_TRIP */
  @Transactional
  fun acceptRide(driverId: UUID, rideId: UUID): com.ridehailing.core_api.common.model.Ride {
    log.info("acceptRide - driverId=$driverId, rideId=$rideId")
    val driver = getDriverById(driverId)

    if (driver.driverStatus != DriverStatus.AVAILABLE) {
      throw AppException(AppExceptionTypes.DRIVER_NOT_AVAILABLE)
    }

    val ride = rideMapper.getById(rideId) ?: throw AppException(AppExceptionTypes.RIDE_NOT_FOUND)

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.ACCEPTED)) {
      throw AppException(AppExceptionTypes.RIDE_ALREADY_TAKEN)
    }

    ride.driverId = driverId
    ride.setStatus(RideStatus.ACCEPTED)
    val updated = rideMapper.updateDriver(ride)
    if (updated == 0) throw AppException(AppExceptionTypes.RIDE_ALREADY_TAKEN)

    driver.setDriverStatus(DriverStatus.ON_TRIP)
    authMapper.updateDriverStatus(driver)
    redisLocationService.markUnavailable(driverId)
    redisLocationService.setActiveRide(driverId, rideId)

    log.info("acceptRide - driver $driverId accepted ride $rideId")

    // Notify rider via SSE
    sseService.send(rideId, RideUpdateEvent().apply {
      this.rideId = ride.id; status = ride.status; this.driverId = driverId
    })

    // Remove this ride from all other drivers' lists
    rideDispatchService.notifyDriversRideRemoved(ride)
    rideQueueService.dequeue(rideId)
    // Close this driver's dispatch SSE (they're now on a trip)
    rideDispatchService.removeDriver(driverId)

    return ride
  }

  /** Decline a ride — record decline, refresh driver's list */
  fun declineRide(driverId: UUID, rideId: UUID) {
    log.info("declineRide - driverId=$driverId, rideId=$rideId")

    val ride = rideMapper.getById(rideId) ?: throw AppException(AppExceptionTypes.RIDE_NOT_FOUND)
    if (ride.status != RideStatus.REQUESTED) {
      throw AppException(AppExceptionTypes.RIDE_ALREADY_TAKEN)
    }

    val decline = RideDecline().apply {
      this.rideId = rideId
      this.driverId = driverId
      this.hash = IdempotencyHash.generate(rideId, driverId)
    }
    rideDeclineMapper.insert(decline)
    log.info("declineRide - recorded decline for ride $rideId by driver $driverId")

    // Refresh this driver's available rides (declined ride will be filtered out)
    rideDispatchService.notifyDriver(driverId)
  }
}
