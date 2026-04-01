package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.Ride
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.UUID

@Mapper
interface RideMapper {
  fun insert(ride: Ride): Int
  fun getById(@Param("id") id: UUID): Ride?
  fun getByIdForUpdate(@Param("id") id: UUID): Ride?
  fun updateStatus(ride: Ride): Int
  fun updateDriver(ride: Ride): Int
  fun updateFare(ride: Ride): Int
  fun updateTripTimes(ride: Ride): Int
  fun getActiveRideForRider(@Param("riderId") riderId: UUID): Ride?
  fun getActiveRideForDriver(@Param("driverId") driverId: UUID): Ride?
  fun hasUnpaidCompletedRide(@Param("riderId") riderId: UUID): Boolean
  fun getUnpaidCompletedRide(@Param("riderId") riderId: UUID): Ride?
  fun getAvailableRidesForDriver(
    @Param("driverId") driverId: UUID,
    @Param("lat") lat: Double,
    @Param("lng") lng: Double
  ): List<Ride>
  fun getStaleRequestedRides(
    @Param("maxRadiusKm") maxRadiusKm: Double,
    @Param("staleSeconds") staleSeconds: Int
  ): List<Ride>
  fun updateSearchRadius(
    @Param("id") id: UUID,
    @Param("searchRadiusKm") searchRadiusKm: Double
  ): Int
}
