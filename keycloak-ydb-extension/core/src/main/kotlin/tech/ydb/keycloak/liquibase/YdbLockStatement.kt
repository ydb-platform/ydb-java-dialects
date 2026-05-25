package tech.ydb.keycloak.liquibase

import liquibase.statement.core.LockDatabaseChangeLogStatement

class YdbLockStatement(val id: Int) : LockDatabaseChangeLogStatement()
