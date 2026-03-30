package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import com.ridehailing.core_api.common.model.Ride
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.ride.dto.CreateRideRequest
import com.ridehailing.core_api.ride.dto.RideResponse
import com.ridehailing.core_api.sse.SSEService
import com.ridehailing.core_api.sse.dto.RideUpdateEvent
import com.ridehailing.core_api.trip.FareCalculator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
open class RideService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var fareCalculator: FareCalculator

  @Autowired
  private lateinit var sseService: SSEService

  @Autowired
  private lateinit var rideDispatchService: RideDispatchService

  /** Create a new ride request and dispatch to nearby drivers */
  @Transactional
  fun createRide(riderId: UUID, request: CreateRideRequest): RideResponse {
    log.info("createRide - riderId=$riderId")
    validateCoordinates(request)

    val existingRide = rideMapper.getActiveRideForRider(riderId)
    if (existingRide != null) {
      throw AppException(AppExceptionTypes.RIDE_ALREADY_ACTIVE)
    }

    if (rideMapper.hasUnpaidCompletedRide(riderId)) {
      throw AppException(AppExceptionTypes.RIDE_UNPAID_EXISTS)
    }

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
      setStatus(RideStatus.REQUESTED)
      this.estimatedFare = estimatedFare
    }
    rideMapper.insert(ride)
    log.info("createRide - ride created id=${ride.id}")

    rideDispatchService.notifyDriversForNewRide(ride)
    return toResponse(ride)
  }

  /** Get active ride for a rider (if any) */
  fun getActiveRide(riderId: UUID): RideResponse? {
    log.info("getActiveRide - riderId=$riderId")
    val ride = rideMapper.getActiveRideForRider(riderId) ?: return null
    return toResponse(ride)
  }

  /** Get ride details with access control */
  fun getRide(rideId: UUID, userId: UUID): RideResponse {
    log.info("getRide - rideId=$rideId, userId=$userId")
    val ride = rideMapper.getById(rideId)
      ?: throw AppException(AppExceptionTypes.RIDE_NOT_FOUND)
    if (ride.riderId != userId && ride.driverId != userId) {
      throw AppException(AppExceptionTypes.RIDE_ACCESS_DENIED)
    }
    return toResponse(ride)
  }

  /** Cancel a ride */
  @Transactional
  fun cancelRide(rideId: UUID, riderId: UUID): RideResponse {
    log.info("cancelRide - rideId=$rideId, riderId=$riderId")
    val ride = rideMapper.getById(rideId)
      ?: throw AppException(AppExceptionTypes.RIDE_NOT_FOUND)

    if (ride.riderId != riderId) throw AppException(AppExceptionTypes.RIDE_CANCEL_FORBIDDEN)

    if (!RideStateMachine.canTransition(ride.status!!, RideStatus.CANCELLED)) {
      throw AppException(AppExceptionTypes.RIDE_INVALID_TRANSITION, ride.status, RideStatus.CANCELLED)
    }

    ride.setStatus(RideStatus.CANCELLED)
    ride.cancelledAt = Instant.now()
    rideMapper.updateStatus(ride)
    log.info("cancelRide - ride $rideId cancelled")

    sseService.send(rideId, RideUpdateEvent().apply {
      this.rideId = ride.id; status = ride.status
    })
    rideDispatchService.notifyDriversRideRemoved(ride)
    return toResponse(ride)
  }

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
    if (errors.isNotEmpty()) throw AppException(AppExceptionTypes.VALIDATION_FAILED, errors)

    if (request.pickupLat!! < -90 || request.pickupLat!! > 90) errors.add("pickupLat must be between -90 and 90")
    if (request.pickupLng!! < -180 || request.pickupLng!! > 180) errors.add("pickupLng must be between -180 and 180")
    if (request.dropoffLat!! < -90 || request.dropoffLat!! > 90) errors.add("dropoffLat must be between -90 and 90")
    if (request.dropoffLng!! < -180 || request.dropoffLng!! > 180) errors.add("dropoffLng must be between -180 and 180")
    if (errors.isNotEmpty()) throw AppException(AppExceptionTypes.VALIDATION_FAILED, errors)
  }
}
