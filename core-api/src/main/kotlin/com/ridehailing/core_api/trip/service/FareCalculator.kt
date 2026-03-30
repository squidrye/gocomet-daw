package com.ridehailing.core_api.trip

import com.ridehailing.core_api.common.util.HaversineUtil
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
open class FareCalculator {

  private val baseFare = BigDecimal("50.00")
  private val perKmRate = BigDecimal("12.00")

  /** Calculate fare based on distance, tier, and surge */
  fun calculate(
    pickupLat: Double, pickupLng: Double,
    dropoffLat: Double, dropoffLng: Double,
    tierMultiplier: Double = 1.0,
    surgeMultiplier: Double = 1.0
  ): BigDecimal {
    val distanceKm = HaversineUtil.distanceKm(pickupLat, pickupLng, dropoffLat, dropoffLng)
    val fare = baseFare + (BigDecimal.valueOf(distanceKm) * perKmRate
      * BigDecimal.valueOf(tierMultiplier) * BigDecimal.valueOf(surgeMultiplier))
    return fare.setScale(2, RoundingMode.HALF_UP)
  }
}
