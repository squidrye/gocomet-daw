package com.ridehailing.core_api.common.model

import java.time.Instant
import java.util.UUID

open class User {
  var id: UUID? = null
  var email: String? = null
  var passwordHash: String? = null

  // DB stores integer type IDs; these are resolved via enum fromId()
  var roleTypeId: Int? = null
  var driverStatusTypeId: Int? = null

  var name: String? = null
  var vehicleMake: String? = null

  var createdAt: Instant? = null
  var updatedAt: Instant? = null

  val role: Role? get() = Role.fromId(roleTypeId)
  val driverStatus: DriverStatus? get() = DriverStatus.fromId(driverStatusTypeId)

  fun setRole(r: Role) { roleTypeId = r.id }
  fun setDriverStatus(ds: DriverStatus?) { driverStatusTypeId = ds?.id }
}
