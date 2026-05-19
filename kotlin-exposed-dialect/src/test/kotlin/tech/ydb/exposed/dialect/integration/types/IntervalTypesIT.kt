package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbInterval
import tech.ydb.exposed.dialect.ydbInterval64
import java.time.Duration

class IntervalTypesIT : BaseYdbTest() {

    object Interval64Types : Table("interval64_types") {
        val id = integer("id")
        val durationCol = ydbInterval64("duration_col")

        override val primaryKey = PrimaryKey(id)
    }

    object LegacyIntervalTypes : Table("legacy_interval_types") {
        val id = integer("id")
        val durationCol = ydbInterval("duration_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Interval64Types, LegacyIntervalTypes)

    @Test
    fun `should round-trip Interval64`() = tx {
        val duration = Duration.ofHours(26).plusMinutes(3).plusSeconds(4)

        Interval64Types.insert {
            it[id] = 1
            it[durationCol] = duration
        }

        val row = Interval64Types.selectAll().single()
        assertEquals(duration, row[Interval64Types.durationCol])
    }

    @Test
    fun `should generate ddl for Interval64`() = tx {
        val ddl = Interval64Types.ddl.joinToString(" ")
        assertTrue(ddl.contains("duration_col Interval64"))
    }

    @Test
    fun `should round-trip legacy Interval`() = tx {
        val duration = Duration.ofDays(1).plusHours(2)

        LegacyIntervalTypes.insert {
            it[id] = 1
            it[durationCol] = duration
        }

        assertEquals(duration, LegacyIntervalTypes.selectAll().single()[LegacyIntervalTypes.durationCol])
    }

    @Test
    fun `should generate ddl for legacy Interval`() = tx {
        val ddl = LegacyIntervalTypes.ddl.joinToString(" ")
        assertTrue(ddl.contains("duration_col Interval") && !ddl.contains("Interval64"))
    }
}
