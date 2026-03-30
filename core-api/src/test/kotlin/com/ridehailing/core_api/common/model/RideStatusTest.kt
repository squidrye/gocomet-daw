package com.ridehailing.core_api.common.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RideStatusTest {

  @Test
  fun fromIdReturnsCorrectStatus() {
    assertEquals(RideStatus.REQUESTED, RideStatus.fromId(7))
    assertEquals(RideStatus.ACCEPTED, RideStatus.fromId(9))
    assertEquals(RideStatus.IN_PROGRESS, RideStatus.fromId(10))
    assertEquals(RideStatus.COMPLETED, RideStatus.fromId(11))
    assertEquals(RideStatus.CANCELLED, RideStatus.fromId(12))
  }

  @Test
  fun fromIdReturnsNullForInvalid() {
    assertNull(RideStatus.fromId(999))
    assertNull(RideStatus.fromId(null))
  }
}
