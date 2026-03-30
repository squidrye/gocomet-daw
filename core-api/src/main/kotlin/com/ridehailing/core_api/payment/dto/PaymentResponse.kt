package com.ridehailing.core_api.payment.dto

import java.math.BigDecimal
import java.util.UUID

open class PaymentResponse {
  var id: UUID? = null
  var rideId: UUID? = null
  var amount: BigDecimal? = null
  var transactionId: String? = null
  var status: String? = null
}
