package com.ridehailing.core_api.driver

import com.ridehailing.core_api.common.model.DriverStatus
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DriverStateMachineTest {

  @Test
  fun offlineCanGoAvailable() {
    assertTrue(DriverStateMachine.canTransition(DriverStatus.OFFLINE, DriverStatus.AVAILABLE))
  }

  @Test
  fun offlineCannotGoOnTrip() {
    assertFalse(DriverStateMachine.canTransition(DriverStatus.OFFLINE, DriverStatus.ON_TRIP))
  }

  @Test
  fun availableCanGoOffline() {
    assertTrue(DriverStateMachine.canTransition(DriverStatus.AVAILABLE, DriverStatus.OFFLINE))
  }

  @Test
  fun availableCanGoOnTrip() {
    assertTrue(DriverStateMachine.canTransition(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP))
  }

  @Test
  fun onTripCanGoAvailable() {
    assertTrue(DriverStateMachine.canTransition(DriverStatus.ON_TRIP, DriverStatus.AVAILABLE))
  }

  @Test
  fun onTripCannotGoOffline() {
    assertFalse(DriverStateMachine.canTransition(DriverStatus.ON_TRIP, DriverStatus.OFFLINE))
  }
}
