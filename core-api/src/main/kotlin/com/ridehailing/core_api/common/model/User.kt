package com.ridehailing.core_api.common.model

import java.time.Instant
import java.util.UUID

open class User {
  var id: UUID? = null
  var email: String? = null
  var passwordHash: String? = null
  var role: Role? = null
  var driverStatus: DriverStatus? = null
  var createdAt: Instant? = null
  var updatedAt: Instant? = null
}
