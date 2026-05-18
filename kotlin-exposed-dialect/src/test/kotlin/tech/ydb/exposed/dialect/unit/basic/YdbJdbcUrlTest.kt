package tech.ydb.exposed.dialect.unit.basic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.ydbJdbcUrl

class YdbJdbcUrlTest {

    @Test
    fun `appends forceSignedDatetimes false for JDBC backward compatibility`() {
        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?forceSignedDatetimes=false",
            ydbJdbcUrl("jdbc:ydb:grpc://localhost:2136/local")
        )
    }

    @Test
    fun `preserves existing query parameters`() {
        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?token=abc&forceSignedDatetimes=false",
            ydbJdbcUrl("jdbc:ydb:grpc://localhost:2136/local?token=abc")
        )
    }

    @Test
    fun `replaces existing driver flag`() {
        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?token=abc&forceSignedDatetimes=false",
            ydbJdbcUrl("jdbc:ydb:grpc://localhost:2136/local?token=abc&forceSignedDatetimes=true")
        )
    }
}
