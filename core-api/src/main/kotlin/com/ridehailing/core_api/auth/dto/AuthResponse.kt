package com.ridehailing.core_api.auth.dto

import com.ridehailing.core_api.common.model.Role
import java.util.UUID

open class AuthResponse {
  var token: String? = null
  var userId: UUID? = null
  var role: Role? = null
}
