package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.RideStatus

/** Enforces valid ride state transitions */
object RideStateMachine {

  private val transitions = mapOf(
    RideStatus.REQUESTED to setOf(RideStatus.MATCHED, RideStatus.CANCELLED),
    RideStatus.MATCHED to setOf(RideStatus.ACCEPTED, RideStatus.CANCELLED),
    RideStatus.ACCEPTED to setOf(RideStatus.IN_PROGRESS, RideStatus.CANCELLED),
    RideStatus.IN_PROGRESS to setOf(RideStatus.COMPLETED),
  )

  fun canTransition(from: RideStatus, to: RideStatus): Boolean =
    transitions[from]?.contains(to) == true
}
