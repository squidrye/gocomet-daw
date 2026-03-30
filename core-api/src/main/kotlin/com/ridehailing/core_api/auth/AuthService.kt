package com.ridehailing.core_api.auth

import com.ridehailing.core_api.auth.dto.AuthResponse
import com.ridehailing.core_api.auth.dto.LoginRequest
import com.ridehailing.core_api.auth.dto.RegisterRequest
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.model.User
import com.ridehailing.core_api.config.JwtService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
open class AuthService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var authMapper: AuthMapper

  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var passwordEncoder: PasswordEncoder

  /** Register a new rider or driver */
  fun register(request: RegisterRequest): AuthResponse {
    log.info("register - email=${request.email}, role=${request.role}")

    val errors = mutableListOf<String>()
    if (request.email.isNullOrBlank()) errors.add("email is required")
    if (request.password.isNullOrBlank()) errors.add("password is required")
    else if (request.password!!.length < 8) errors.add("password must be at least 8 characters")
    if (request.role == null) errors.add("role is required")
    if (errors.isNotEmpty()) {
      throw AppException(
        status = HttpStatus.BAD_REQUEST,
        message = "Validation failed",
        details = errors
      )
    }

    val existing = authMapper.getByEmail(request.email!!)
    if (existing != null) {
      throw AppException(
        status = HttpStatus.CONFLICT,
        message = "Email already registered"
      )
    }

    val user = User().apply {
      email = request.email
      passwordHash = passwordEncoder.encode(request.password)
      role = request.role
    }
    authMapper.insert(user)

    val token = jwtService.generateToken(user.id!!, user.role!!)
    log.info("register - created userId=${user.id}")

    return AuthResponse().apply {
      this.token = token
      this.userId = user.id
      this.role = user.role
    }
  }

  /** Login with email and password */
  fun login(request: LoginRequest): AuthResponse {
    log.info("login - email=${request.email}")

    val errors = mutableListOf<String>()
    if (request.email.isNullOrBlank()) errors.add("email is required")
    if (request.password.isNullOrBlank()) errors.add("password is required")
    if (errors.isNotEmpty()) {
      throw AppException(
        status = HttpStatus.BAD_REQUEST,
        message = "Validation failed",
        details = errors
      )
    }

    val user = authMapper.getByEmail(request.email!!)
      ?: throw AppException(
        status = HttpStatus.UNAUTHORIZED,
        message = "Invalid credentials"
      )

    if (!passwordEncoder.matches(request.password, user.passwordHash)) {
      throw AppException(
        status = HttpStatus.UNAUTHORIZED,
        message = "Invalid credentials"
      )
    }

    val token = jwtService.generateToken(user.id!!, user.role!!)
    log.info("login - authenticated userId=${user.id}")

    return AuthResponse().apply {
      this.token = token
      this.userId = user.id
      this.role = user.role
    }
  }
}
