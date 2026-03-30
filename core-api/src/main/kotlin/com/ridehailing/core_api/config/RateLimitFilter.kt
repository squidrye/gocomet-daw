package com.ridehailing.core_api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap

@Component
open class RateLimitFilter : OncePerRequestFilter() {

  private val log = LoggerFactory.getLogger(this::class.java)

  private val buckets = ConcurrentHashMap<String, TokenBucket>()
  private val maxTokens = 60L
  private val refillRatePerSec = 1L

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val ip = request.remoteAddr
    val bucket = buckets.computeIfAbsent(ip) { TokenBucket(maxTokens, refillRatePerSec) }

    if (bucket.tryConsume()) {
      filterChain.doFilter(request, response)
    } else {
      log.debug("doFilterInternal - rate limit exceeded for ip=$ip")
      response.status = 429
      response.contentType = "application/json"
      response.writer.write("""{"error":"Too many requests"}""")
    }
  }

  class TokenBucket(private val maxTokens: Long, private val refillRatePerSec: Long) {
    private var tokens: Double = maxTokens.toDouble()
    private var lastRefill: Long = System.nanoTime()

    @Synchronized
    fun tryConsume(): Boolean {
      refill()
      if (tokens >= 1) {
        tokens -= 1
        return true
      }
      return false
    }

    private fun refill() {
      val now = System.nanoTime()
      val elapsed = (now - lastRefill) / 1_000_000_000.0
      tokens = minOf(maxTokens.toDouble(), tokens + elapsed * refillRatePerSec)
      lastRefill = now
    }
  }
}
