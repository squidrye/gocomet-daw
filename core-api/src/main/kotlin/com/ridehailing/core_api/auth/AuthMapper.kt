package com.ridehailing.core_api.auth

import com.ridehailing.core_api.common.model.User
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.UUID

@Mapper
interface AuthMapper {
  fun insert(user: User): Int
  fun getByEmail(@Param("email") email: String): User?
  fun getById(@Param("id") id: UUID): User?
  fun updateDriverStatus(user: User): Int
}
