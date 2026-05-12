package tech.ydb.exposed.dialect.unit.basic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.ydbUlid

class YdbGeneratedIdsTest {

    @Test
    fun `ydbUlid generates a 26-char Crockford-base32 string`() {
        val value = ydbUlid(nowMillis = 1_700_000_000_000)
        assertEquals(26, value.length)
        assertTrue(value.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" })
    }

    @Test
    fun `ydbUlid encodes timestamp in the lexicographic prefix`() {
        val older = ydbUlid(nowMillis = 1_700_000_000_000)
        val newer = ydbUlid(nowMillis = 1_700_000_000_001)
        assertTrue(older.take(10) < newer.take(10))
    }
}
