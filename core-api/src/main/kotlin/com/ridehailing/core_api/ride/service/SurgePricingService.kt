package com.ridehailing.core_api.ride

import com.ridehailing.core_api.driver.RedisDriverLocationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class SurgePricingService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var redisLocationService: RedisDriverLocationService

  fun calculateMultiplier(pickupLat: Double, pickupLng: Double): Double {
    log.info("calculateMultiplier - lat=$pickupLat, lng=$pickupLng")
    val nearbyDrivers = redisLocationService.findAvailableDriversNear(pickupLat, pickupLng, 5.0).size

    return when {
      nearbyDrivers == 0 -> 2.0
      nearbyDrivers <= 2 -> 1.5
      nearbyDrivers <= 5 -> 1.2
      else -> 1.0
    }
  }
}
