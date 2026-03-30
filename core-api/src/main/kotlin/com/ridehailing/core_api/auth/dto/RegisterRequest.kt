package com.ridehailing.core_api.auth.dto

import com.ridehailing.core_api.common.model.Role

open class RegisterRequest {
  var email: String? = null
  var password: String? = null
  var role: Role? = null
}
