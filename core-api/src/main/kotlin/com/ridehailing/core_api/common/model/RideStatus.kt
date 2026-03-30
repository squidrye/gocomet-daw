package com.ridehailing.core_api.common.model

enum class RideStatus(val id: Int) {
  REQUESTED(7),
  MATCHED(8),
  ACCEPTED(9),
  IN_PROGRESS(10),
  COMPLETED(11),
  CANCELLED(12);

  companion object {
    fun fromId(id: Int?): RideStatus? = entries.find { it.id == id }
  }
}
