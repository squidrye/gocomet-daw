package com.ridehailing.core_api.config

import com.ridehailing.core_api.common.model.Role
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
open class JwtService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Value("\${jwt.secret}")
  private lateinit var secret: String

  @Value("\${jwt.expiration-ms}")
  private var expirationMs: Long = 86400000

  /** Generate JWT with userId as subject and role as claim */
  fun generateToken(userId: UUID, role: Role): String {
    log.debug("generateToken - userId=$userId, role=$role")
    val now = Date()
    val expiry = Date(now.time + expirationMs)
    val key = Keys.hmacShaKeyFor(secret.toByteArray())
    return Jwts.builder()
      .subject(userId.toString())
      .claim("role", role.name)
      .issuedAt(now)
      .expiration(expiry)
      .signWith(key)
      .compact()
  }

  /** Extract userId from token subject */
  fun extractUserId(token: String): UUID? {
    return try {
      val key = Keys.hmacShaKeyFor(secret.toByteArray())
      val subject = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .payload
        .subject
      UUID.fromString(subject)
    } catch (e: Exception) {
      log.debug("extractUserId - failed: ${e.message}")
      null
    }
  }

  /** Extract role claim from token */
  fun extractRole(token: String): Role? {
    return try {
      val key = Keys.hmacShaKeyFor(secret.toByteArray())
      val roleName = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .payload
        .get("role", String::class.java)
      Role.valueOf(roleName)
    } catch (e: Exception) {
      log.debug("extractRole - failed: ${e.message}")
      null
    }
  }

  /** Check if token is valid (signature + not expired) */
  fun isTokenValid(token: String): Boolean {
    return try {
      val key = Keys.hmacShaKeyFor(secret.toByteArray())
      Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
      true
    } catch (e: Exception) {
      log.debug("isTokenValid - invalid: ${e.message}")
      false
    }
  }
}
