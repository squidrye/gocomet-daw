package com.ridehailing.core_api.driver

import com.ridehailing.core_api.common.model.DriverStatus

/** Enforces valid driver state transitions */
object DriverStateMachine {

  private val transitions = mapOf(
    DriverStatus.OFFLINE to setOf(DriverStatus.AVAILABLE),
    DriverStatus.AVAILABLE to setOf(DriverStatus.OFFLINE, DriverStatus.ON_TRIP),
    DriverStatus.ON_TRIP to setOf(DriverStatus.AVAILABLE),
  )

  fun canTransition(from: DriverStatus, to: DriverStatus): Boolean =
    transitions[from]?.contains(to) == true
}
