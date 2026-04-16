package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.types.ydbUuid
import tech.ydb.exposed.dialect.types.ydbUuidBytes
import tech.ydb.exposed.dialect.types.ydbUuidUtf8
import java.util.UUID

class UuidTypesIT : BaseYdbTest() {

    object NativeUuidTypes : YdbTable("native_uuid_types") {
        val id = integer("id")
        val uuidCol = ydbUuid("uuid_col")

        override val primaryKey = PrimaryKey(id)
    }

    object UuidUtf8Types : YdbTable("uuid_utf8_types") {
        val id = integer("id")
        val uuidCol = ydbUuidUtf8("uuid_col")

        override val primaryKey = PrimaryKey(id)
    }

    object UuidBytesTypes : YdbTable("uuid_bytes_types") {
        val id = integer("id")
        val uuidCol = ydbUuidBytes("uuid_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(NativeUuidTypes, UuidUtf8Types, UuidBytesTypes)

    @Test
    fun `should round-trip native uuid type`() = tx {
        val uuid = UUID.randomUUID()

        NativeUuidTypes.insert {
            it[id] = 1
            it[uuidCol] = uuid
        }

        val row = NativeUuidTypes.selectAll().single()
        assertEquals(uuid, row[NativeUuidTypes.uuidCol])
    }

    @Test
    fun `should round-trip uuid utf8 type`() = tx {
        val uuid = UUID.randomUUID()

        UuidUtf8Types.insert {
            it[id] = 1
            it[uuidCol] = uuid
        }

        val row = UuidUtf8Types.selectAll().single()
        assertEquals(uuid, row[UuidUtf8Types.uuidCol])
    }

    @Test
    fun `should round-trip uuid bytes type`() = tx {
        val uuid = UUID.randomUUID()

        UuidBytesTypes.insert {
            it[id] = 1
            it[uuidCol] = uuid
        }

        val row = UuidBytesTypes.selectAll().single()
        assertEquals(uuid, row[UuidBytesTypes.uuidCol])
    }

    @Test
    fun `should generate ddl for uuid mappings`() = tx {
        val nativeDdl = NativeUuidTypes.ddl.joinToString(" ")
        val utf8Ddl = UuidUtf8Types.ddl.joinToString(" ")
        val bytesDdl = UuidBytesTypes.ddl.joinToString(" ")

        assertTrue(nativeDdl.contains("uuid_col Uuid"))
        assertTrue(utf8Ddl.contains("uuid_col Utf8"))
        assertTrue(bytesDdl.contains("uuid_col String"))
    }
}