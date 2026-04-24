package tech.ydb.exposed.dialect.integration.ttl

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbDialect
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class TtlAlterSqlIT : BaseYdbTest() {

    object AlterTtlItems : YdbTable("alter_ttl_items") {
        val id = integer("id")
        val expireAt = timestamp("expire_at")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(expireAt, "PT24H")
        }
    }

    override val tables: List<Table> = listOf(AlterTtlItems)

    @Test
    fun `should generate alter ttl sql`() = tx {
        val dialect = db.dialect as YdbDialect

        val setSql = dialect.setTtl(AlterTtlItems)
        val resetSql = dialect.resetTtl(AlterTtlItems)

        assertTrue(setSql.contains("""SET (TTL = Interval("PT24H") ON expire_at)"""))
        assertTrue(resetSql.contains("RESET (TTL)"))
    }
}