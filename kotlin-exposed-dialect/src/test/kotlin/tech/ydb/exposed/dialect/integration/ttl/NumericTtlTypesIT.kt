package tech.ydb.exposed.dialect.integration.ttl

import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.basic.YdbTtlColumnMode
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.types.ydbUint64

class NumericTtlTypesIT : BaseYdbTest() {

    object NumericTtlItems : YdbTable("numeric_ttl_items") {
        val id = integer("id")
        val modifiedAtEpoch = ydbUint64("modified_at_epoch")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(modifiedAtEpoch, "PT1H", YdbTtlColumnMode.SECONDS)
        }
    }

    override val tables: List<Table> = listOf(NumericTtlItems)

    @Test
    fun `should generate ttl for numeric epoch column`() = tx {
        val ddl = NumericTtlItems.ddl.joinToString(" ")

        assertTrue(
            ddl.contains("""WITH (TTL = Interval("PT1H") ON modified_at_epoch AS SECONDS)""")
        )
        assertTrue(ddl.contains("modified_at_epoch Uint64"))
    }
}