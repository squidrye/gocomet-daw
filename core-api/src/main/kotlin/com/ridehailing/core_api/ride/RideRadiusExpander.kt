package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.RideStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
open class RideRadiusExpander {

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val RADIUS_INCREMENT_KM = 5.0
    const val MAX_RADIUS_KM = 20.0
  }

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var rideQueueService: RideQueueService

  @Autowired
  private lateinit var rideDispatchService: RideDispatchService

  @Scheduled(fixedDelay = 10000, initialDelay = 15000)
  fun processQueue() {
    if (!rideQueueService.tryAcquireLock()) return

    try {
      val dueRideIds = rideQueueService.popDueRides()
      if (dueRideIds.isEmpty()) return

      log.info("processQueue - processing ${dueRideIds.size} rides for expansion")
      dueRideIds.forEach { expandAndRedispatch(it) }
    } finally {
      rideQueueService.releaseLock()
    }
  }

  private fun expandAndRedispatch(rideId: UUID) {
    val ride = rideMapper.getById(rideId)
    if (ride == null || ride.status != RideStatus.REQUESTED) {
      log.debug("expandAndRedispatch - ride $rideId no longer REQUESTED, skipping")
      return
    }

    val currentRadius = ride.searchRadiusKm ?: 5.0
    val newRadius = (currentRadius + RADIUS_INCREMENT_KM).coerceAtMost(MAX_RADIUS_KM)

    rideMapper.updateSearchRadius(rideId, newRadius)
    ride.searchRadiusKm = newRadius
    log.info("expandAndRedispatch - ride $rideId radius ${currentRadius}km -> ${newRadius}km")

    rideDispatchService.notifyDriversForNewRide(ride)

    if (newRadius < MAX_RADIUS_KM) {
      rideQueueService.enqueue(rideId)
    }
  }
}
