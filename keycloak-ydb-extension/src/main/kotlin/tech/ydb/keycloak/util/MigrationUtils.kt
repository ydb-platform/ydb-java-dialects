package tech.ydb.keycloak.util

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun hikariDataSource(
  jdbcUrl: String?,
  poolSize: Int,
): HikariDataSource = HikariDataSource(hikariConfig(jdbcUrl, poolSize))

fun hikariConfig(
  jdbcUrl: String?,
  poolSize: Int,
): HikariConfig = HikariConfig().apply {// todo Review how to create connections correctly.
  this.jdbcUrl = jdbcUrl
  this.driverClassName = "tech.ydb.jdbc.YdbDriver"
  this.maximumPoolSize = poolSize
  this.poolName = "YDB-HikariPool"
  this.isAutoCommit = false // todo review
}
