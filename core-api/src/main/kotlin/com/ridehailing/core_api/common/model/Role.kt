package com.ridehailing.core_api.common.model

enum class Role(val id: Int) {
  RIDER(1),
  DRIVER(2);

  companion object {
    fun fromId(id: Int?): Role? = entries.find { it.id == id }
  }
}
