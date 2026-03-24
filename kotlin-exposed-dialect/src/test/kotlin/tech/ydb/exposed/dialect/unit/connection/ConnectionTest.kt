package tech.ydb.exposed.dialect.unit.connection

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbDialectProvider

class ConnectionTest {
    @Test
    fun `should connect to YDB`() {
        val db = YdbDialectProvider.connect(
            url = "jdbc:ydb:grpc://localhost:2136/local",
            driver = "tech.ydb.jdbc.YdbDriver"
        )

        Assertions.assertNotNull(db)
    }
}