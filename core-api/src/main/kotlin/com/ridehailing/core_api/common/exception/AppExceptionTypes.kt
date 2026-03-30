package com.ridehailing.core_api.common.exception

import org.springframework.http.HttpStatus

/**
 * Central registry of all application exception triplets.
 * Format: Triple(errorCode, HttpStatus, message)
 */
object AppExceptionTypes {

  // Generic
  val GENERIC_ERROR = Triple(1, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.")
  val RESOURCE_NOT_FOUND = Triple(2, HttpStatus.NOT_FOUND, "Resource not found")
  val UNAUTHORIZED = Triple(3, HttpStatus.UNAUTHORIZED, "Unauthorized")
  val FORBIDDEN = Triple(4, HttpStatus.FORBIDDEN, "Access denied")
  val VALIDATION_FAILED = Triple(5, HttpStatus.BAD_REQUEST, "Validation failed")
  val MALFORMED_REQUEST = Triple(6, HttpStatus.BAD_REQUEST, "Malformed request body")
  val TOO_MANY_REQUESTS = Triple(7, HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please slow down.")

  // Auth
  val EMAIL_ALREADY_EXISTS = Triple(10, HttpStatus.CONFLICT, "Email already registered")
  val INVALID_CREDENTIALS = Triple(11, HttpStatus.UNAUTHORIZED, "Invalid credentials")
  val USER_NOT_FOUND = Triple(12, HttpStatus.NOT_FOUND, "User not found")
  val NOT_A_DRIVER = Triple(13, HttpStatus.CONFLICT, "User is not a driver")

  // Ride
  val RIDE_NOT_FOUND = Triple(20, HttpStatus.NOT_FOUND, "Ride not found")
  val RIDE_ACCESS_DENIED = Triple(21, HttpStatus.FORBIDDEN, "You do not have access to this ride")
  val RIDE_INVALID_TRANSITION = Triple(22, HttpStatus.CONFLICT, "Invalid ride state transition: %s → %s")
  val RIDE_CANCEL_FORBIDDEN = Triple(23, HttpStatus.FORBIDDEN, "Only the rider can cancel this ride")
  val NO_DRIVERS_AVAILABLE = Triple(24, HttpStatus.OK, "No drivers available nearby")
  val RIDE_ALREADY_ACTIVE = Triple(25, HttpStatus.CONFLICT, "You already have an active ride")

  // Driver
  val DRIVER_NOT_FOUND = Triple(30, HttpStatus.NOT_FOUND, "Driver not found")
  val DRIVER_INVALID_TRANSITION = Triple(31, HttpStatus.CONFLICT, "Invalid driver state transition: %s → %s")
  val DRIVER_NOT_LOCKED = Triple(32, HttpStatus.CONFLICT, "Driver is not in LOCKED state")
  val NO_PENDING_OFFER = Triple(33, HttpStatus.NOT_FOUND, "No pending offer found")
  val DRIVER_LOCATION_UNAVAILABLE = Triple(34, HttpStatus.NOT_FOUND, "Driver location not available")
  val DRIVER_LOCATION_FORBIDDEN =
    Triple(35, HttpStatus.FORBIDDEN, "Driver location only available for ACCEPTED or IN_PROGRESS rides")

  // Trip
  val TRIP_INVALID_TRANSITION = Triple(40, HttpStatus.CONFLICT, "Cannot transition trip in %s state")
  val TRIP_DRIVER_MISMATCH = Triple(41, HttpStatus.FORBIDDEN, "Driver not assigned to this ride")
  val RIDE_NO_FINAL_FARE = Triple(42, HttpStatus.CONFLICT, "Ride has no final fare recorded")

  // Payment
  val PAYMENT_RIDE_NOT_COMPLETED = Triple(50, HttpStatus.CONFLICT, "Payment can only be processed for completed rides")
  val PAYMENT_ACCESS_DENIED = Triple(51, HttpStatus.FORBIDDEN, "You do not have access to this payment")
}
