package tech.ydb.exposed.dialect

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test


class ConnectionTest {
    @Test
    fun `should connect to YDB`() {
        val db = YdbDialectProvider.connect(
            url = "jdbc:ydb:grpc://localhost:2136/local",
            driver = "tech.ydb.jdbc.YdbDriver"
        )

        assertNotNull(db)
    }
}