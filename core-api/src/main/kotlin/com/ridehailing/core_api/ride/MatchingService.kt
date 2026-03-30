package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.DriverLocation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
open class MatchingService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var rideMapper: RideMapper

  /** Find nearest available driver within 5km radius */
  fun findNearestDriver(pickupLat: Double, pickupLng: Double, excludeDriverIds: List<UUID>? = null): DriverLocation? {
    log.info("findNearestDriver - pickupLat=$pickupLat, pickupLng=$pickupLng, excludeCount=${excludeDriverIds?.size ?: 0}")
    val driver = rideMapper.findNearestDriver(pickupLat, pickupLng, 5.0, excludeDriverIds)
    if (driver != null) {
      log.info("findNearestDriver - found driverId=${driver.driverId}")
    } else {
      log.info("findNearestDriver - no available driver found")
    }
    return driver
  }
}
