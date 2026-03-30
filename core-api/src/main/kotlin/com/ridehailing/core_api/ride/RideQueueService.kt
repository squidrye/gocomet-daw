package com.ridehailing.core_api.ride

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
open class RideQueueService {

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val QUEUE_KEY = "ride:pending_queue"
    const val LOCK_KEY = "ride:queue_lock"
    const val EXPAND_DELAY_MS = 30_000L
  }

  @Autowired
  private lateinit var redisTemplate: StringRedisTemplate

  fun enqueue(rideId: UUID) {
    val expandAt = System.currentTimeMillis() + EXPAND_DELAY_MS
    redisTemplate.opsForZSet().add(QUEUE_KEY, rideId.toString(), expandAt.toDouble())
    log.info("enqueue - ride $rideId queued for expansion at $expandAt")
  }

  fun dequeue(rideId: UUID) {
    redisTemplate.opsForZSet().remove(QUEUE_KEY, rideId.toString())
    log.debug("dequeue - ride $rideId removed from queue")
  }

  fun popDueRides(): List<UUID> {
    val now = System.currentTimeMillis().toDouble()
    val members = redisTemplate.opsForZSet().rangeByScore(QUEUE_KEY, 0.0, now) ?: return emptyList()
    if (members.isEmpty()) return emptyList()
    members.forEach { redisTemplate.opsForZSet().remove(QUEUE_KEY, it) }
    return members.map { UUID.fromString(it) }
  }

  fun tryAcquireLock(ttl: Duration = Duration.ofSeconds(10)): Boolean {
    return redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", ttl) == true
  }

  fun releaseLock() {
    redisTemplate.delete(LOCK_KEY)
  }
}
