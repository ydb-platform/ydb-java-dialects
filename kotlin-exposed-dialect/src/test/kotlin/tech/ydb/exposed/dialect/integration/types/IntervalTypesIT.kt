package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.types.ydbInterval
import java.time.Duration

class IntervalTypesIT : BaseYdbTest() {

    object IntervalTypes : YdbTable("interval_types") {
        val id = integer("id")
        val durationCol = ydbInterval("duration_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(IntervalTypes)

    @Test
    fun `should round-trip interval type`() = tx {
        val duration = Duration.ofHours(26).plusMinutes(3).plusSeconds(4)

        IntervalTypes.insert {
            it[id] = 1
            it[durationCol] = duration
        }

        val row = IntervalTypes.selectAll().single()
        assertEquals(duration, row[IntervalTypes.durationCol])
    }

    @Test
    fun `should generate ddl for interval type`() = tx {
        val ddl = IntervalTypes.ddl.joinToString(" ")
        assertTrue(ddl.contains("duration_col Interval"))
    }
}