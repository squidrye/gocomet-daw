package com.ridehailing.core_api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
open class SecurityHeadersFilter : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    response.setHeader("X-Content-Type-Options", "nosniff")
    response.setHeader("X-Frame-Options", "DENY")
    response.setHeader("X-XSS-Protection", "1; mode=block")
    response.setHeader("Cache-Control", "no-store")
    response.setHeader("Content-Security-Policy", "default-src 'self'")
    filterChain.doFilter(request, response)
  }
}
