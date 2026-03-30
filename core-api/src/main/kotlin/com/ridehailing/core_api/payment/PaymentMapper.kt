package com.ridehailing.core_api.payment

import com.ridehailing.core_api.common.model.Payment
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.UUID

@Mapper
interface PaymentMapper {
  fun insert(payment: Payment): Int
  fun getByRideId(@Param("rideId") rideId: UUID): Payment?
}
