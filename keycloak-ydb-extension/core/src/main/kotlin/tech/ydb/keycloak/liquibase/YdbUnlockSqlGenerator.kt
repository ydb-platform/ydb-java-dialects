package tech.ydb.keycloak.liquibase

import liquibase.database.Database
import liquibase.exception.ValidationErrors
import liquibase.sql.Sql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.sqlgenerator.SqlGeneratorFactory
import liquibase.sqlgenerator.core.AbstractSqlGenerator
import liquibase.statement.core.DeleteStatement
import tech.ydb.liquibase.database.YdbDatabase

/**
 * Generates a DELETE-based lock release for YDB.
 *
 * The lock table invariant is: presence of a row = locked, absence = unlocked.
 * Deleting the row releases the lock and makes it available for the next process.
 */
class YdbUnlockSqlGenerator : AbstractSqlGenerator<YdbUnlockStatement>() {

    override fun getPriority(): Int = PRIORITY_DATABASE

    override fun supports(statement: YdbUnlockStatement, database: Database): Boolean =
        database is YdbDatabase

    override fun validate(
        statement: YdbUnlockStatement,
        database: Database,
        chain: SqlGeneratorChain<YdbUnlockStatement>
    ): ValidationErrors = ValidationErrors()

    override fun generateSql(
        statement: YdbUnlockStatement,
        database: Database,
        chain: SqlGeneratorChain<YdbUnlockStatement>
    ): Array<Sql> {
        val deleteStatement = DeleteStatement(
            database.liquibaseCatalogName,
            database.liquibaseSchemaName,
            database.databaseChangeLogLockTableName
        ).setWhere("ID = ${statement.id}")

        return SqlGeneratorFactory.getInstance().generateSql(deleteStatement, database)
    }
}
