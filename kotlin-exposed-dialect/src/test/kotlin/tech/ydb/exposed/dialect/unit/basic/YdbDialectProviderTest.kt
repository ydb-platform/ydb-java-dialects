package tech.ydb.exposed.dialect.unit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialectProvider

class YdbDialectProviderTest {

    @Test
    fun `default temporal mode enables driver flag for new datetime types`() {
        val actual = YdbDialectProvider.withTemporalDriverMode(
            url = "jdbc:ydb:grpc://localhost:2136/local",
            forceLegacyDatetimes = false
        )

        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?forceSignedDatetimes=true",
            actual
        )
    }

    @Test
    fun `legacy temporal mode disables driver flag`() {
        val actual = YdbDialectProvider.withTemporalDriverMode(
            url = "jdbc:ydb:grpc://localhost:2136/local",
            forceLegacyDatetimes = true
        )

        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?forceSignedDatetimes=false",
            actual
        )
    }

    @Test
    fun `temporal mode preserves existing query parameters`() {
        val actual = YdbDialectProvider.withTemporalDriverMode(
            url = "jdbc:ydb:grpc://localhost:2136/local?token=abc",
            forceLegacyDatetimes = false
        )

        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?token=abc&forceSignedDatetimes=true",
            actual
        )
    }

    @Test
    fun `temporal mode replaces existing driver flag`() {
        val actual = YdbDialectProvider.withTemporalDriverMode(
            url = "jdbc:ydb:grpc://localhost:2136/local?token=abc&forceSignedDatetimes=false",
            forceLegacyDatetimes = false
        )

        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?token=abc&forceSignedDatetimes=true",
            actual
        )
    }
}
