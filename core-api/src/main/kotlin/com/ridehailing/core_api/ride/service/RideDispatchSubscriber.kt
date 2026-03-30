package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
open class RideDispatchSubscriber {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  @Lazy
  private lateinit var rideDispatchService: RideDispatchService

  fun onMessage(message: String) {
    try {
      val event = JsonUtil.objectMapper.readValue(message, DispatchEvent::class.java)
      log.debug("onMessage - type=${event.type}, rideId=${event.rideId}")

      when (event.type) {
        DispatchEventType.NEW_RIDE,
        DispatchEventType.RIDE_REMOVED,
        DispatchEventType.RADIUS_EXPANDED -> rideDispatchService.handleDispatchEvent(event)

        DispatchEventType.DRIVER_CONNECTED,
        DispatchEventType.DRIVER_DECLINED -> event.driverId?.let { rideDispatchService.handleDriverConnected(it) }

        null -> log.warn("onMessage - received event with null type")
      }
    } catch (e: Exception) {
      log.error("onMessage - failed: ${e.message}")
    }
  }
}
