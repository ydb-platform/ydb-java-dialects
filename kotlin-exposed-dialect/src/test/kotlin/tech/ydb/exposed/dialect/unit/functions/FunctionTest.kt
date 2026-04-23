package tech.ydb.exposed.dialect.unit.functions

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider

class FunctionTest {
    private val provider = YdbFunctionProvider()

    @Test
    fun `should generate limit only`() {
        val sql = provider.queryLimitAndOffset(size = 10, offset = 0, alreadyOrdered = false)
        assertTrue(sql.contains("LIMIT 10"))
    }

    @Test
    fun `should generate limit and offset`() {
        val sql = provider.queryLimitAndOffset(size = 10, offset = 5, alreadyOrdered = false)
        assertTrue(sql.contains("LIMIT 10"))
        assertTrue(sql.contains("OFFSET 5"))
    }

    @Test
    fun `should generate offset without limit`() {
        val sql = provider.queryLimitAndOffset(size = null, offset = 5, alreadyOrdered = false)
        assertTrue(sql.contains("OFFSET 5"))
    }
}
