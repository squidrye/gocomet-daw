package com.ridehailing.core_api.driver

import com.ridehailing.core_api.common.model.DriverLocation
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.UUID

@Mapper
interface DriverLocationMapper {
  fun insert(location: DriverLocation): Int
  fun bulkInsert(@Param("locations") locations: List<DriverLocation>): Int
  fun getLatestByDriverId(@Param("driverId") driverId: UUID): DriverLocation?
}
