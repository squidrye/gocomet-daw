package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.Ride
import com.ridehailing.core_api.common.util.JsonUtil
import com.ridehailing.core_api.config.RedisConfig
import com.ridehailing.core_api.driver.RedisDriverLocationService
import com.ridehailing.core_api.ride.dto.AvailableRideResponse
import com.ridehailing.core_api.sse.SSEService
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

  @Autowired
  private lateinit var sseService: SSEService

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

  fun notifyDriverMoved(driverId: UUID) {
    publish(DispatchEvent().apply {
      type = DispatchEventType.DRIVER_MOVED
      this.driverId = driverId
    })
  }

  fun publishDriverLocation(rideId: UUID, lat: Double, lng: Double) {
    publish(DispatchEvent().apply {
      type = DispatchEventType.DRIVER_LOCATION
      this.rideId = rideId
      this.driverLat = lat
      this.driverLng = lng
    })
  }

  fun handleDispatchEvent(event: DispatchEvent) {
    val lat = event.pickupLat ?: return
    val lng = event.pickupLng ?: return
    val radius = event.searchRadiusKm ?: DEFAULT_RADIUS_KM

    val nearbyDrivers = redisLocationService.findAvailableDriversNear(lat, lng, radius)
    log.debug("handleDispatchEvent - type=${event.type}, nearby=${nearbyDrivers.size}")

    when (event.type) {
      DispatchEventType.NEW_RIDE, DispatchEventType.RADIUS_EXPANDED -> {
        val ride = event.rideId?.let { rideMapper.getById(it) } ?: return
        val payload = toAvailableRide(ride)
        nearbyDrivers.forEach { (driverId, _) ->
          pushSingleRideToDriver(driverId, "ride-added", payload)
        }
      }
      DispatchEventType.RIDE_REMOVED -> {
        nearbyDrivers.forEach { (driverId, _) ->
          pushSingleRideToDriver(driverId, "ride-removed", mapOf("rideId" to event.rideId))
        }
      }
      else -> {}
    }
  }

  fun handleDriverConnected(driverId: UUID) {
    val loc = redisLocationService.getLocation(driverId) ?: return
    pushRideListToDriver(driverId, loc.first, loc.second)
  }

  fun handleDriverLocation(rideId: UUID, lat: Double, lng: Double) {
    sseService.sendDriverLocation(rideId, lat, lng)
  }

  private fun pushSingleRideToDriver(driverId: UUID, eventName: String, payload: Any) {
    val emitter = driverEmitters[driverId] ?: return
    try {
      emitter.send(
        SseEmitter.event()
          .name(eventName)
          .data(payload, MediaType.APPLICATION_JSON)
      )
    } catch (e: Exception) {
      log.debug("pushSingleRideToDriver - failed for $driverId, removing emitter")
      driverEmitters.remove(driverId)
    }
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
      surgeMultiplier = ride.surgeMultiplier
    }
  }
}
