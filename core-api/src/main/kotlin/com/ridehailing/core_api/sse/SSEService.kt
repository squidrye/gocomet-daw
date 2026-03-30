package com.ridehailing.core_api.sse

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
open class SSEService {

  private val log = LoggerFactory.getLogger(this::class.java)

  private val emitters = ConcurrentHashMap<UUID, SseEmitter>()

  fun register(rideId: UUID): SseEmitter {
    log.info("register - rideId=$rideId")
    val emitter = SseEmitter(0L)
    emitters[rideId] = emitter
    emitter.onCompletion { emitters.remove(rideId) }
    emitter.onTimeout { emitters.remove(rideId) }
    emitter.onError { emitters.remove(rideId) }
    return emitter
  }

  fun send(rideId: UUID, eventData: Any) {
    emitters[rideId]?.let { emitter ->
      try {
        emitter.send(
          SseEmitter.event()
            .name("ride-update")
            .data(eventData, MediaType.APPLICATION_JSON)
        )
      } catch (e: Exception) {
        log.debug("send - failed for rideId=$rideId, removing emitter")
        emitters.remove(rideId)
      }
    }
  }
}
