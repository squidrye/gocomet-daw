package com.ridehailing.core_api.auth

import com.ridehailing.core_api.auth.dto.AuthResponse
import com.ridehailing.core_api.auth.dto.LoginRequest
import com.ridehailing.core_api.auth.dto.RegisterRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
open class AuthController {

  @Autowired
  private lateinit var authService: AuthService

  @Operation(summary = "Register a new rider or driver")
  @PostMapping("/register")
  fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
    val response = authService.register(request)
    return ResponseEntity.status(HttpStatus.CREATED).body(response)
  }

  @Operation(summary = "Login with email and password")
  @PostMapping("/login")
  fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
    val response = authService.login(request)
    return ResponseEntity.ok(response)
  }
}
