package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.RideStatus
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RideStateMachineTest {

  @Test
  fun requestedCanTransitionToAccepted() {
    assertTrue(RideStateMachine.canTransition(RideStatus.REQUESTED, RideStatus.ACCEPTED))
  }

  @Test
  fun requestedCanTransitionToCancelled() {
    assertTrue(RideStateMachine.canTransition(RideStatus.REQUESTED, RideStatus.CANCELLED))
  }

  @Test
  fun requestedCannotTransitionToCompleted() {
    assertFalse(RideStateMachine.canTransition(RideStatus.REQUESTED, RideStatus.COMPLETED))
  }

  @Test
  fun requestedCannotTransitionToInProgress() {
    assertFalse(RideStateMachine.canTransition(RideStatus.REQUESTED, RideStatus.IN_PROGRESS))
  }

  @Test
  fun acceptedCanTransitionToInProgress() {
    assertTrue(RideStateMachine.canTransition(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS))
  }

  @Test
  fun acceptedCanTransitionToCancelled() {
    assertTrue(RideStateMachine.canTransition(RideStatus.ACCEPTED, RideStatus.CANCELLED))
  }

  @Test
  fun inProgressCanTransitionToCompleted() {
    assertTrue(RideStateMachine.canTransition(RideStatus.IN_PROGRESS, RideStatus.COMPLETED))
  }

  @Test
  fun inProgressCannotTransitionToCancelled() {
    assertFalse(RideStateMachine.canTransition(RideStatus.IN_PROGRESS, RideStatus.CANCELLED))
  }

  @Test
  fun completedCannotTransitionAnywhere() {
    RideStatus.entries.forEach { target ->
      assertFalse(RideStateMachine.canTransition(RideStatus.COMPLETED, target))
    }
  }

  @Test
  fun cancelledCannotTransitionAnywhere() {
    RideStatus.entries.forEach { target ->
      assertFalse(RideStateMachine.canTransition(RideStatus.CANCELLED, target))
    }
  }
}
