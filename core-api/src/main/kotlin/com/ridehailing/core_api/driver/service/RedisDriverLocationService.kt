package com.ridehailing.core_api.driver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.geo.Circle
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Point
import org.springframework.data.redis.connection.RedisGeoCommands
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
open class RedisDriverLocationService {

  companion object {
    const val GEO_KEY = "driver:locations"
    const val AVAILABLE_KEY = "driver:available"
  }

  @Autowired
  private lateinit var redisTemplate: StringRedisTemplate

  fun updateLocation(driverId: UUID, lat: Double, lng: Double) {
    redisTemplate.opsForGeo().add(GEO_KEY, Point(lng, lat), driverId.toString())
  }

  fun getLocation(driverId: UUID): Pair<Double, Double>? {
    val positions = redisTemplate.opsForGeo().position(GEO_KEY, driverId.toString())
    val pos = positions?.firstOrNull() ?: return null
    return Pair(pos.y, pos.x)
  }

  fun markAvailable(driverId: UUID) {
    redisTemplate.opsForSet().add(AVAILABLE_KEY, driverId.toString())
  }

  fun setActiveRide(driverId: UUID, rideId: UUID) {
    redisTemplate.opsForValue().set("driver:ride:$driverId", rideId.toString())
  }

  fun getActiveRideId(driverId: UUID): UUID? {
    val value = redisTemplate.opsForValue().get("driver:ride:$driverId") ?: return null
    return UUID.fromString(value)
  }

  fun clearActiveRide(driverId: UUID) {
    redisTemplate.delete("driver:ride:$driverId")
  }

  fun markUnavailable(driverId: UUID) {
    redisTemplate.opsForSet().remove(AVAILABLE_KEY, driverId.toString())
  }

  fun findAvailableDriversNear(lat: Double, lng: Double, radiusKm: Double): List<Pair<UUID, Double>> {
    val results = redisTemplate.opsForGeo().radius(
      GEO_KEY,
      Circle(Point(lng, lat), Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS)),
      RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
        .includeDistance()
        .sortAscending()
        .limit(100)
    ) ?: return emptyList()

    val availableSet = redisTemplate.opsForSet().members(AVAILABLE_KEY) ?: emptySet()

    return results.content
      .filter { it.content.name in availableSet }
      .map { Pair(UUID.fromString(it.content.name), it.distance.value) }
  }

  fun removeDriver(driverId: UUID) {
    redisTemplate.opsForGeo().remove(GEO_KEY, driverId.toString())
    redisTemplate.opsForSet().remove(AVAILABLE_KEY, driverId.toString())
  }
}
