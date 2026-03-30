package com.ridehailing.core_api.driver.dto

import com.ridehailing.core_api.common.model.DriverStatus
import java.util.UUID

open class DriverStatusResponse {
  var userId: UUID? = null
  var driverStatus: DriverStatus? = null
}
