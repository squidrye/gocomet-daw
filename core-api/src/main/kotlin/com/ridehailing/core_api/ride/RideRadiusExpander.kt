package com.ridehailing.core_api.ride

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Periodically expands search radius for REQUESTED rides that haven't
 * been picked up by any driver. Bumps radius by 5km each cycle up to max.
 *
 * In production, this would be replaced by a Redis-backed delayed queue
 * or SQS with visibility timeout for multi-instance support.
 */
@Component
open class RideRadiusExpander {

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val RADIUS_INCREMENT_KM = 5.0
    const val MAX_RADIUS_KM = 20.0
    const val STALE_THRESHOLD_SECONDS = 30
  }

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var rideDispatchService: RideDispatchService

  /** Runs every 30s — finds stale REQUESTED rides and expands their radius */
  @Scheduled(fixedDelay = 30000, initialDelay = 30000)
  fun expandStaleRides() {
    val staleRides = rideMapper.getStaleRequestedRides(MAX_RADIUS_KM, STALE_THRESHOLD_SECONDS)
    if (staleRides.isEmpty()) return

    log.info("expandStaleRides - found ${staleRides.size} stale rides to expand")
    staleRides.forEach { ride ->
      val newRadius = (ride.searchRadiusKm ?: 5.0) + RADIUS_INCREMENT_KM
      rideMapper.updateSearchRadius(ride.id!!, newRadius.coerceAtMost(MAX_RADIUS_KM))
      ride.searchRadiusKm = newRadius.coerceAtMost(MAX_RADIUS_KM)
      log.info("expandStaleRides - ride ${ride.id} radius expanded to ${ride.searchRadiusKm}km")
      rideDispatchService.notifyDriversForNewRide(ride)
    }
  }
}
