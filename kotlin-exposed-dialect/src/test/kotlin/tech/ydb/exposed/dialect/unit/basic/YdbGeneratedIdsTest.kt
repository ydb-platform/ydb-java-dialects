package tech.ydb.exposed.dialect.unit.basic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbGeneratedIds
import java.util.UUID

class YdbGeneratedIdsTest {

    @Test
    fun `should generate uuid string`() {
        val value = YdbGeneratedIds.uuidString()

        UUID.fromString(value)
        assertEquals(36, value.length)
    }

    @Test
    fun `should generate ulid with stable length and alphabet`() {
        val value = YdbGeneratedIds.ulid(nowMillis = 1_700_000_000_000)

        assertEquals(26, value.length)
        assertTrue(value.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" })
    }

    @Test
    fun `should encode ulid timestamp in lexicographic prefix`() {
        val older = YdbGeneratedIds.ulid(nowMillis = 1_700_000_000_000)
        val newer = YdbGeneratedIds.ulid(nowMillis = 1_700_000_000_001)

        assertTrue(older.take(10) < newer.take(10))
    }
}