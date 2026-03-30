package com.ridehailing.core_api.common.model

enum class PaymentStatus(val id: Int) {
  SUCCESS(13),
  FAILED(14);

  companion object {
    fun fromId(id: Int?): PaymentStatus? = entries.find { it.id == id }
  }
}
