package com.ridehailing.core_api.trip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FareCalculatorTest {

  private val calculator = FareCalculator()

  @Test
  fun baseFareForZeroDistance() {
    val fare = calculator.calculate(19.076, 72.877, 19.076, 72.877)
    assertEquals(BigDecimal("50.00"), fare)
  }

  @Test
  fun fareIncreasesWithDistance() {
    val shortFare = calculator.calculate(19.076, 72.877, 19.080, 72.880)
    val longFare = calculator.calculate(19.076, 72.877, 19.100, 72.900)
    assertTrue(longFare > shortFare)
  }

  @Test
  fun surgeMultiplierIncreasesFare() {
    val normalFare = calculator.calculate(19.076, 72.877, 19.100, 72.900)
    val surgeFare = calculator.calculate(19.076, 72.877, 19.100, 72.900, surgeMultiplier = 2.0)
    assertTrue(surgeFare > normalFare)
  }

  @Test
  fun fareHasTwoDecimalPlaces() {
    val fare = calculator.calculate(19.076, 72.877, 19.100, 72.900)
    assertEquals(2, fare.scale())
  }

  @Test
  fun noSurgeMultiplierDefaultsToOne() {
    val fare1 = calculator.calculate(19.076, 72.877, 19.100, 72.900)
    val fare2 = calculator.calculate(19.076, 72.877, 19.100, 72.900, surgeMultiplier = 1.0)
    assertEquals(fare1, fare2)
  }
}
