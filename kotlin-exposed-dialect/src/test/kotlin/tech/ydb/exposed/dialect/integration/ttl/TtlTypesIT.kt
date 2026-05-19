package tech.ydb.exposed.dialect.integration.ttl

import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.javatime.ydbTimestamp

class TtlTypesIT : BaseYdbTest() {

    object ExpiringItems : YdbTable("expiring_items") {
        val id = integer("id")
        val expireAt = ydbTimestamp("expire_at")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(expireAt, "PT1H")
        }
    }

    override val tables: List<Table> = listOf(ExpiringItems)

    @Test
    fun `should generate ttl for timestamp column`() = tx {
        val ddl = ExpiringItems.ddl.joinToString(" ")

        assertTrue(ddl.contains("""WITH (TTL = Interval("PT1H") ON expire_at)"""))
        assertTrue(ddl.contains("PRIMARY KEY (id)"))
    }
}