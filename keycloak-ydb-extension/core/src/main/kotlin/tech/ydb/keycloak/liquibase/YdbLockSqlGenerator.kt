package tech.ydb.keycloak.liquibase

import liquibase.database.Database
import liquibase.exception.ValidationErrors
import liquibase.sql.Sql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.sqlgenerator.SqlGeneratorFactory
import liquibase.sqlgenerator.core.LockDatabaseChangeLogGenerator
import liquibase.statement.DatabaseFunction
import liquibase.statement.core.InsertStatement
import liquibase.statement.core.LockDatabaseChangeLogStatement
import tech.ydb.liquibase.database.YdbDatabase

/**
 * Generates an INSERT-based lock acquisition for YDB.
 *
 * YDB does not support SELECT FOR UPDATE. Instead, we use INSERT: inserting
 * a row succeeds only when no row with that ID exists (lock is free). If the row
 * already exists (lock is held), the INSERT fails with a primary-key violation,
 * which the caller catches and interprets as "lock busy, retry later".
 *
 * Lock table invariant: absence of a row means "unlocked"; presence means "locked".
 * The table is kept empty during init — see YdbLockService.init().
 * Release is done by DELETE — see YdbUnlockSqlGenerator.
 *
 * Extends LockDatabaseChangeLogGenerator to inherit hostname/hostaddress/hostDescription
 * static fields
 */
class YdbLockSqlGenerator : LockDatabaseChangeLogGenerator() {

    override fun getPriority(): Int = PRIORITY_DATABASE

    override fun supports(statement: LockDatabaseChangeLogStatement, database: Database): Boolean =
        statement is YdbLockStatement && database is YdbDatabase

    override fun validate(
        statement: LockDatabaseChangeLogStatement,
        database: Database,
        chain: SqlGeneratorChain<*>
    ): ValidationErrors = ValidationErrors()

    override fun generateSql(
        statement: LockDatabaseChangeLogStatement,
        database: Database,
        chain: SqlGeneratorChain<*>
    ): Array<Sql> {
        statement as YdbLockStatement

        val insertStatement = InsertStatement(
            database.liquibaseCatalogName,
            database.liquibaseSchemaName,
            database.databaseChangeLogLockTableName
        )
            .addColumnValue("ID", statement.id)
            .addColumnValue("LOCKED", true)
            .addColumnValue("LOCKGRANTED", DatabaseFunction("CurrentUtcDatetime()"))
            .addColumnValue("LOCKEDBY", "$hostname$hostDescription ($hostaddress)")

        return SqlGeneratorFactory.getInstance().generateSql(insertStatement, database)
    }
}
