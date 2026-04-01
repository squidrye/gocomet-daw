package com.ridehailing.core_api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
open class RequestCachingFilter : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val wrapper = if (request is ContentCachingRequestWrapper) request
      else ContentCachingRequestWrapper(request)
    filterChain.doFilter(wrapper, response)
  }
}
