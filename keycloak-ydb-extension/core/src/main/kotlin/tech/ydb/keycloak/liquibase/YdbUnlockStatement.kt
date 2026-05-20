package tech.ydb.keycloak.liquibase

import liquibase.statement.core.UnlockDatabaseChangeLogStatement

class YdbUnlockStatement(val id: Int) : UnlockDatabaseChangeLogStatement()
