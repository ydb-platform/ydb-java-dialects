package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbUuid
import java.util.UUID

class UuidTypesIT : BaseYdbTest() {

    object NativeUuidTypes : YdbTable("native_uuid_types") {
        val id = integer("id")
        val uuidCol = ydbUuid("uuid_col")
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(NativeUuidTypes)

    @Test
    fun `ydbUuid round-trips a java util UUID through native YDB Uuid (no string conversion)`() = tx {
        val uuid = UUID.randomUUID()

        NativeUuidTypes.insert {
            it[id] = 1
            it[uuidCol] = uuid
        }

        assertEquals(uuid, NativeUuidTypes.selectAll().single()[NativeUuidTypes.uuidCol])
    }

    @Test
    fun `DDL emits Uuid for ydbUuid`() = tx {
        assertTrue(NativeUuidTypes.ddl.joinToString(" ").contains("uuid_col Uuid"))
    }
}
