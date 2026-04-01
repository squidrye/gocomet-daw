package com.ridehailing.core_api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Component
open class RateLimitFilter : OncePerRequestFilter() {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var redisTemplate: StringRedisTemplate

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val key = resolveKey(request)
    val limit = resolveLimit(request)

    val current = redisTemplate.opsForValue().increment(key) ?: 1
    if (current == 1L) {
      // first request then set ttl on key
      redisTemplate.expire(key, Duration.ofMinutes(1))
    }

    if (current <= limit) {
      filterChain.doFilter(request, response)
    } else {
      log.debug("doFilterInternal - rate limit exceeded for key=$key, count=$current, limit=$limit")
      response.status = 429
      response.contentType = "application/json"
      response.writer.write("""{"error":"Too many requests"}""")
    }
  }

  private fun resolveKey(request: HttpServletRequest): String {
    val group = getEndpointGroup(request)
    val token = extractToken(request)
    if (token != null) {
      val userId = try { jwtService.extractUserId(token) } catch (_: Exception) { null }
      if (userId != null) return "ratelimit:user:$userId:$group"
    }
    return "ratelimit:ip:${request.remoteAddr}:$group"
  }

  private fun resolveLimit(request: HttpServletRequest): Long {
    return when (getEndpointGroup(request)) {
      "auth" -> 10
      "location" -> 200
      "rides" -> 30
      "payments" -> 10
      else -> 60
    }
  }

  private fun getEndpointGroup(request: HttpServletRequest): String {
    val path = request.requestURI.removePrefix("/v1/")
    return when {
      path.startsWith("auth") -> "auth"
      path.startsWith("drivers/me/location") -> "location"
      path.startsWith("rides") || path.startsWith("drivers/me/rides") -> "rides"
      path.startsWith("payments") -> "payments"
      else -> "default"
    }
  }

  private fun extractToken(request: HttpServletRequest): String? {
    val header = request.getHeader("Authorization")
    if (header != null && header.startsWith("Bearer ")) return header.substring(7)
    return request.getParameter("token")
  }
}
