package com.ridehailing.core_api.common.util

import java.util.UUID

object IdempotencyHash {

  fun generate(vararg fields: Any?): String {
    val input = fields.joinToString("|") { it?.toString() ?: "" }
    return UUID.nameUUIDFromBytes(input.toByteArray()).toString()
  }
}
