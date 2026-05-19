package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import tech.ydb.exposed.dialect.YdbRetryConfig
import tech.ydb.exposed.dialect.ydbTransaction

class YdbTransactionOperations internal constructor(
    private val database: Database
) {

    fun <T> execute(
        retry: YdbRetryConfig = YdbRetryConfig.DEFAULT,
        readOnly: Boolean = false,
        statement: JdbcTransaction.() -> T
    ): T = ydbTransaction(
        db = database,
        retry = retry,
        readOnly = readOnly,
        statement = statement
    )
}
