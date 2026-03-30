package com.ridehailing.core_api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
open class JwtAuthFilter : OncePerRequestFilter() {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var jwtService: JwtService

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val token = extractToken(request)

    if (token != null && jwtService.isTokenValid(token)) {
      val userId = jwtService.extractUserId(token)
      val role = jwtService.extractRole(token)

      if (userId != null && role != null) {
        log.debug("doFilterInternal - authenticated userId=$userId, role=$role")
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
        val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
        SecurityContextHolder.getContext().authentication = auth
      }
    }

    filterChain.doFilter(request, response)
  }

  /** Extract token from Authorization header or query param */
  private fun extractToken(request: HttpServletRequest): String? {
    val header = request.getHeader("Authorization")
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7)
    }
    // Fallback: query param for SSE endpoints
    return request.getParameter("token")
  }
}
