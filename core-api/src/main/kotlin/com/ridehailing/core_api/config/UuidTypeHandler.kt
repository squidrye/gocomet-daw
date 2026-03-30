package com.ridehailing.core_api.config

import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID

@MappedTypes(UUID::class)
open class UuidTypeHandler : BaseTypeHandler<UUID>() {

  override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: UUID, jdbcType: JdbcType?) {
    ps.setObject(i, parameter)
  }

  override fun getNullableResult(rs: ResultSet, columnName: String): UUID? {
    val value = rs.getObject(columnName)
    return value as? UUID
  }

  override fun getNullableResult(rs: ResultSet, columnIndex: Int): UUID? {
    val value = rs.getObject(columnIndex)
    return value as? UUID
  }

  override fun getNullableResult(cs: CallableStatement, columnIndex: Int): UUID? {
    val value = cs.getObject(columnIndex)
    return value as? UUID
  }
}
