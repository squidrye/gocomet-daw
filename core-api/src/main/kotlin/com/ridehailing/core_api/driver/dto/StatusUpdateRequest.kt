package com.ridehailing.core_api.driver.dto

import com.ridehailing.core_api.common.model.DriverStatus

open class StatusUpdateRequest {
  var status: DriverStatus? = null
}
