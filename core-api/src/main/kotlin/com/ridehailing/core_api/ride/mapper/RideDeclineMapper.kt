package com.ridehailing.core_api.ride

import com.ridehailing.core_api.common.model.RideDecline
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.UUID

@Mapper
interface RideDeclineMapper {
  fun insert(decline: RideDecline): Int
  fun getDeclinedRideIdsForDriver(@Param("driverId") driverId: UUID): List<UUID>
}
