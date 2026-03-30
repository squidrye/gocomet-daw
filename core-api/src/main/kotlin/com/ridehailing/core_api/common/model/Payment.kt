package com.ridehailing.core_api.common.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

open class Payment {
  var id: UUID? = null
  var rideId: UUID? = null
  var amount: BigDecimal? = null
  var transactionId: String? = null
  var status: String? = null
  var createdAt: Instant? = null
}
