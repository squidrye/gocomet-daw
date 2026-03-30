package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.Ride
import com.ridehailing.core_api.common.util.JsonUtil
import com.ridehailing.core_api.config.RedisConfig
import com.ridehailing.core_api.driver.RedisDriverLocationService
import com.ridehailing.core_api.ride.dto.AvailableRideResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
open class RideDispatchService {

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val DEFAULT_RADIUS_KM = 5.0
  }

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var redisLocationService: RedisDriverLocationService

  @Autowired
  private lateinit var redisTemplate: StringRedisTemplate

  private val driverEmitters = ConcurrentHashMap<UUID, SseEmitter>()

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

  fun removeDriver(driverId: UUID) {
    driverEmitters.remove(driverId)?.let {
      try { it.complete() } catch (_: Exception) {}
    }
  }

  fun isDriverConnected(driverId: UUID): Boolean = driverEmitters.containsKey(driverId)

  fun notifyDriversForNewRide(ride: Ride) {
    publish(DispatchEvent().apply {
      type = DispatchEventType.NEW_RIDE
      rideId = ride.id
      pickupLat = ride.pickupLat
      pickupLng = ride.pickupLng
      searchRadiusKm = ride.searchRadiusKm ?: DEFAULT_RADIUS_KM
    })
  }

  fun notifyDriverOnConnect(driverId: UUID) {
    publish(DispatchEvent().apply {
      type = DispatchEventType.DRIVER_CONNECTED
      this.driverId = driverId
    })
  }

  fun notifyDriversRideRemoved(ride: Ride) {
    publish(DispatchEvent().apply {
      type = DispatchEventType.RIDE_REMOVED
      rideId = ride.id
      pickupLat = ride.pickupLat
      pickupLng = ride.pickupLng
      searchRadiusKm = DEFAULT_RADIUS_KM
    })
  }

  fun notifyDriver(driverId: UUID) {
    publish(DispatchEvent().apply {
      type = DispatchEventType.DRIVER_DECLINED
      this.driverId = driverId
    })
  }

  fun handleDispatchEvent(event: DispatchEvent) {
    val lat = event.pickupLat ?: return
    val lng = event.pickupLng ?: return
    val radius = event.searchRadiusKm ?: DEFAULT_RADIUS_KM

    val nearbyDrivers = redisLocationService.findAvailableDriversNear(lat, lng, radius)
    log.debug("handleDispatchEvent - type=${event.type}, nearby=${nearbyDrivers.size}")

    nearbyDrivers.forEach { (driverId, _) ->
      val loc = redisLocationService.getLocation(driverId)
      if (loc != null) pushRideListToDriver(driverId, loc.first, loc.second)
    }
  }

  fun handleDriverConnected(driverId: UUID) {
    val loc = redisLocationService.getLocation(driverId) ?: return
    pushRideListToDriver(driverId, loc.first, loc.second)
  }

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
    } catch (e: Exception) {
      log.debug("pushRideListToDriver - failed for $driverId, removing emitter")
      driverEmitters.remove(driverId)
    }
  }

  private fun publish(event: DispatchEvent) {
    redisTemplate.convertAndSend(RedisConfig.DISPATCH_CHANNEL, JsonUtil.toJson(event))
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
