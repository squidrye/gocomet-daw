package com.ridehailing.core_api.common.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

open class Payment {
  var id: UUID? = null
  var rideId: UUID? = null
  var amount: BigDecimal? = null
  var transactionId: String? = null

  // DB stores integer type ID; resolved via enum fromId()
  var statusTypeId: Int? = null

  var createdAt: Instant? = null
  var hash: String? = null

  val status: PaymentStatus? get() = PaymentStatus.fromId(statusTypeId)

  fun setStatus(s: PaymentStatus) { statusTypeId = s.id }
}
