package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.DriverLocation
import com.ridehailing.core_api.common.model.Ride
import com.ridehailing.core_api.driver.DriverLocationMapper
import com.ridehailing.core_api.ride.dto.AvailableRideResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates ride dispatch to drivers via SSE.
 * Designed as a single coordination point — in production this layer
 * would be backed by Redis Pub/Sub for multi-instance support.
 */
@Service
open class RideDispatchService {

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val RADIUS_KM = 5.0
  }

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var driverLocationMapper: DriverLocationMapper

  /** driverId → SSE emitter for pushing available rides */
  private val driverEmitters = ConcurrentHashMap<UUID, SseEmitter>()

  /** Register a driver's SSE connection */
  fun registerDriver(driverId: UUID): SseEmitter {
    log.info("registerDriver - driverId=$driverId")
    removeDriver(driverId)
    val emitter = SseEmitter(0L)
    driverEmitters[driverId] = emitter
    emitter.onCompletion { driverEmitters.remove(driverId) }
    emitter.onTimeout { driverEmitters.remove(driverId) }
    emitter.onError { driverEmitters.remove(driverId) }
    return emitter
  }

  /** Unregister a driver (going offline) */
  fun removeDriver(driverId: UUID) {
    driverEmitters.remove(driverId)?.let {
      try { it.complete() } catch (_: Exception) {}
    }
  }

  /** Check if a driver has an active SSE connection */
  fun isDriverConnected(driverId: UUID): Boolean = driverEmitters.containsKey(driverId)

  /** Triggered when a new ride is created — notify all nearby available drivers */
  fun notifyDriversForNewRide(ride: Ride) {
    log.info("notifyDriversForNewRide - rideId=${ride.id}")
    val nearbyDrivers = rideMapper.getAvailableDriversNearLocation(
      ride.pickupLat!!, ride.pickupLng!!, ride.searchRadiusKm ?: RADIUS_KM
    )
    log.info("notifyDriversForNewRide - found ${nearbyDrivers.size} nearby drivers")
    nearbyDrivers.forEach { dl ->
      pushRideListToDriver(dl.driverId!!, dl.latitude!!, dl.longitude!!)
    }
  }

  /** Triggered when a driver comes online — push pending rides near them */
  fun notifyDriverOnConnect(driverId: UUID) {
    log.info("notifyDriverOnConnect - driverId=$driverId")
    val location = driverLocationMapper.getLatestByDriverId(driverId)
    if (location == null) {
      log.info("notifyDriverOnConnect - no location for driver, skipping")
      return
    }
    pushRideListToDriver(driverId, location.latitude!!, location.longitude!!)
  }

  /** Triggered when a ride is accepted or cancelled — remove from all drivers' lists */
  fun notifyDriversRideRemoved(ride: Ride) {
    log.info("notifyDriversRideRemoved - rideId=${ride.id}")
    val nearbyDrivers = rideMapper.getAvailableDriversNearLocation(
      ride.pickupLat!!, ride.pickupLng!!, RADIUS_KM
    )
    nearbyDrivers.forEach { dl ->
      pushRideListToDriver(dl.driverId!!, dl.latitude!!, dl.longitude!!)
    }
  }

  /** Triggered when a driver declines — refresh just that driver's list */
  fun notifyDriver(driverId: UUID) {
    log.info("notifyDriver - driverId=$driverId")
    val location = driverLocationMapper.getLatestByDriverId(driverId)
    if (location == null) return
    pushRideListToDriver(driverId, location.latitude!!, location.longitude!!)
  }

  /** Core method: query available rides for a driver and push via SSE */
  private fun pushRideListToDriver(driverId: UUID, lat: Double, lng: Double) {
    val emitter = driverEmitters[driverId] ?: return
    val rides = rideMapper.getAvailableRidesForDriver(driverId, lat, lng)
    val payload = rides.map { toAvailableRide(it) }
    try {
      emitter.send(
        SseEmitter.event()
          .name("available-rides")
          .data(payload, MediaType.APPLICATION_JSON)
      )
      log.debug("pushRideListToDriver - pushed ${payload.size} rides to driver $driverId")
    } catch (e: Exception) {
      log.debug("pushRideListToDriver - failed for driver $driverId, removing emitter")
      driverEmitters.remove(driverId)
    }
  }

  private fun toAvailableRide(ride: Ride): AvailableRideResponse {
    return AvailableRideResponse().apply {
      rideId = ride.id
      pickupLat = ride.pickupLat
      pickupLng = ride.pickupLng
      dropoffLat = ride.dropoffLat
      dropoffLng = ride.dropoffLng
      estimatedFare = ride.estimatedFare
    }
  }
}
