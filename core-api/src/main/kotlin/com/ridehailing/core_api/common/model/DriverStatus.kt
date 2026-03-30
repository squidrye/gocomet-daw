package com.ridehailing.core_api.common.model

enum class DriverStatus(val id: Int) {
  OFFLINE(3),
  AVAILABLE(4),
  ON_TRIP(6);

  companion object {
    fun fromId(id: Int?): DriverStatus? = entries.find { it.id == id }
  }
}
