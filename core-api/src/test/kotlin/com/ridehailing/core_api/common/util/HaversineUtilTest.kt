package com.ridehailing.core_api.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HaversineUtilTest {

  @Test
  fun samePointReturnsZero() {
    val d = HaversineUtil.distanceKm(19.076, 72.877, 19.076, 72.877)
    assertEquals(0.0, d, 0.001)
  }

  @Test
  fun knownDistanceMumbaiToPune() {
    val d = HaversineUtil.distanceKm(19.076, 72.877, 18.520, 73.856)
    assertTrue(d in 110.0..130.0, "Mumbai to Pune should be ~120km, got $d")
  }

  @Test
  fun shortDistanceWithinCity() {
    val d = HaversineUtil.distanceKm(19.076, 72.877, 19.080, 72.880)
    assertTrue(d < 1.0, "Points ~500m apart should be < 1km, got $d")
  }

  @Test
  fun symmetricDistance() {
    val d1 = HaversineUtil.distanceKm(19.076, 72.877, 18.520, 73.856)
    val d2 = HaversineUtil.distanceKm(18.520, 73.856, 19.076, 72.877)
    assertEquals(d1, d2, 0.001)
  }
}
