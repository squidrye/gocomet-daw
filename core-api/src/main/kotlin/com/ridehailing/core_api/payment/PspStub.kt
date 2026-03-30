package com.ridehailing.core_api.payment

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
open class PspStub {

  /** Stub PSP — always returns success */
  fun processPayment(amount: BigDecimal): String {
    return UUID.randomUUID().toString()
  }
}
