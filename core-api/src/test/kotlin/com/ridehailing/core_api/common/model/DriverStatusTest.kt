package com.ridehailing.core_api.common.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DriverStatusTest {

  @Test
  fun fromIdReturnsCorrectStatus() {
    assertEquals(DriverStatus.OFFLINE, DriverStatus.fromId(3))
    assertEquals(DriverStatus.AVAILABLE, DriverStatus.fromId(4))
    assertEquals(DriverStatus.ON_TRIP, DriverStatus.fromId(6))
  }

  @Test
  fun fromIdReturnsNullForInvalid() {
    assertNull(DriverStatus.fromId(999))
    assertNull(DriverStatus.fromId(null))
  }
}
