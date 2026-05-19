package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.LiteralOp
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbFunctionProvider
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

/**
 * End-to-end checks for [tech.ydb.exposed.dialect.YdbDataTypeProvider.hexToDb].
 *
 * Exposed calls it from [BlobColumnType.nonNullValueToString] (e.g. [LiteralOp] or inline UPSERT values).
 */
class BinaryHexToDbIT : BaseYdbTest() {

    object BinaryHex : Table("binary_hex_to_db") {
        val id = integer("id")
        val payload = blob("payload")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(BinaryHex)

    @Test
    fun `select with LiteralOp ExposedBlob uses hexToDb in SQL`() = tx {
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val literal = LiteralOp(BlobColumnType(), ExposedBlob(bytes))

        BinaryHex.insert {
            it[id] = 1
            it[payload] = ExposedBlob(bytes)
        }

        BinaryHex.insert {
            it[id] = 2
            it[payload] = ExposedBlob(byteArrayOf(0x00, 0x01))
        }

        val sql = BinaryHex
            .selectAll()
            .where { BinaryHex.payload eq literal }
            .prepareSQL(this, prepared = false)

        assertTrue(sql.contains("String::HexDecode('deadbeef')"), "SQL: $sql")

        val rows = BinaryHex.selectAll().where { BinaryHex.payload eq literal }.toList()

        assertEquals(1, rows.size)
        assertArrayEquals(bytes, rows.single()[BinaryHex.payload].bytes)
    }

    @Test
    fun `upsert with ExposedBlob embeds hexToDb literal and round-trips`() = tx {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val upsertSql = YdbFunctionProvider.upsert(
            table = BinaryHex,
            data = listOf(
                BinaryHex.id to 1,
                BinaryHex.payload to ExposedBlob(bytes)
            ),
            expression = "",
            onUpdate = emptyList(),
            keyColumns = emptyList(),
            where = null,
            transaction = this
        )

        assertTrue(
            upsertSql.contains("String::HexDecode('01020304')"),
            "UPSERT SQL should use hexToDb: $upsertSql"
        )

        exec(upsertSql)

        val row = BinaryHex.selectAll().single()
        assertArrayEquals(bytes, row[BinaryHex.payload].bytes)
    }
}
