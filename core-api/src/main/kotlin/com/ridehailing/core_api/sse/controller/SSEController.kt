package com.ridehailing.core_api.sse

import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.driver.DriverService
import com.ridehailing.core_api.driver.dto.DriverLocationResponse
import com.ridehailing.core_api.ride.RideMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/rides")
@Tag(name = "Rides")
open class SSEController {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var sseService: SSEService

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var driverService: DriverService

  @Operation(summary = "SSE stream for ride status updates")
  @GetMapping("/{id}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun streamEvents(@PathVariable id: UUID): SseEmitter {
    log.info("streamEvents - rideId=$id")
    return sseService.register(id)
  }
}
