package com.ridehailing.core_api.auth

import com.ridehailing.core_api.auth.dto.AuthResponse
import com.ridehailing.core_api.auth.dto.LoginRequest
import com.ridehailing.core_api.auth.dto.RegisterRequest
import com.ridehailing.core_api.common.util.IdempotencyHash
import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import com.ridehailing.core_api.common.model.Role
import com.ridehailing.core_api.common.model.User
import com.ridehailing.core_api.config.JwtService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

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
  @Transactional
  fun register(request: RegisterRequest): AuthResponse {
    log.info("register - email=${request.email}, role=${request.role}")

    val errors = mutableListOf<String>()
    if (request.email.isNullOrBlank()) errors.add("email is required")
    if (request.password.isNullOrBlank()) errors.add("password is required")
    else if (request.password!!.length < 8) errors.add("password must be at least 8 characters")
    if (request.role == null) errors.add("role is required")
    if (request.name.isNullOrBlank()) errors.add("name is required")
    if (request.role == Role.DRIVER && request.vehicleMake.isNullOrBlank()) errors.add("vehicleMake is required for drivers")
    if (errors.isNotEmpty()) throw AppException(AppExceptionTypes.VALIDATION_FAILED, errors)

    if (authMapper.getByEmail(request.email!!) != null) {
      throw AppException(AppExceptionTypes.EMAIL_ALREADY_EXISTS)
    }

    val user = User().apply {
      email = request.email
      passwordHash = passwordEncoder.encode(request.password)
      setRole(request.role!!)
      name = request.name
      vehicleMake = request.vehicleMake
      hash = IdempotencyHash.generate(request.email)
    }
    authMapper.insert(user)
    log.info("register - created userId=${user.id}")

    val token = jwtService.generateToken(user.id!!, user.role!!)
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
    if (errors.isNotEmpty()) throw AppException(AppExceptionTypes.VALIDATION_FAILED, errors)

    val user = authMapper.getByEmail(request.email!!)
      ?: throw AppException(AppExceptionTypes.INVALID_CREDENTIALS)

    if (!passwordEncoder.matches(request.password, user.passwordHash)) {
      throw AppException(AppExceptionTypes.INVALID_CREDENTIALS)
    }

    log.info("login - authenticated userId=${user.id}")
    val token = jwtService.generateToken(user.id!!, user.role!!)
    return AuthResponse().apply {
      this.token = token
      this.userId = user.id
      this.role = user.role
    }
  }
}
