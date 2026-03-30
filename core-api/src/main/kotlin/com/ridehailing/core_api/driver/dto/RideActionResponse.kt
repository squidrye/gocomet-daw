package com.ridehailing.core_api.driver.dto

import com.ridehailing.core_api.common.model.RideStatus
import java.util.UUID

open class RideActionResponse {
  var rideId: UUID? = null
  var status: RideStatus? = null
}
