package com.ridehailing.core_api.driver

import com.ridehailing.core_api.common.model.DriverLocation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedQueue

@Service
open class LocationAuditBuffer {

  private val log = LoggerFactory.getLogger(this::class.java)
  private val buffer = ConcurrentLinkedQueue<DriverLocation>()

  @Autowired
  private lateinit var driverLocationMapper: DriverLocationMapper

  fun add(location: DriverLocation) {
    buffer.add(location)
  }

  @Scheduled(fixedDelay = 5000, initialDelay = 5000)
  fun flush() {
    val batch = mutableListOf<DriverLocation>()
    while (batch.size < 500) {
      val item = buffer.poll() ?: break
      batch.add(item)
    }
    if (batch.isEmpty()) return
    try {
      driverLocationMapper.bulkInsert(batch)
      log.debug("flush - inserted ${batch.size} location records")
    } catch (e: Exception) {
      log.error("flush - failed to bulk insert: ${e.message}")
    }
  }
}
