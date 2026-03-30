package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.DriverLocation
import com.ridehailing.core_api.common.model.Ride
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.UUID

@Mapper
interface RideMapper {
  fun insert(ride: Ride): Int
  fun getById(@Param("id") id: UUID): Ride?
  fun updateStatus(ride: Ride): Int
  fun updateDriver(ride: Ride): Int
  fun updateFare(ride: Ride): Int
  fun findNearestDriver(
    @Param("pickupLat") pickupLat: Double,
    @Param("pickupLng") pickupLng: Double,
    @Param("radiusKm") radiusKm: Double,
    @Param("excludeDriverIds") excludeDriverIds: List<UUID>?
  ): DriverLocation?
  fun getMatchedRideForDriver(@Param("driverId") driverId: UUID): Ride?
  fun getActiveRideForRider(@Param("riderId") riderId: UUID): Ride?
  fun updateTripTimes(ride: Ride): Int
}
