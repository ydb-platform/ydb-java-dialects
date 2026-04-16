package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class TypesIT : BaseYdbTest() {

    object BasicTypes : YdbTable("basic_types") {
        val id = integer("id")
        val shortCol = short("short_col")
        val intCol = integer("int_col")
        val longCol = long("long_col")
        val boolCol = bool("bool_col")
        val floatCol = float("float_col")
        val doubleCol = double("double_col")
        val varcharCol = varchar("varchar_col", 255)
        val textCol = text("text_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(BasicTypes)

    @Test
    fun `should round-trip basic scalar types`() = tx {
        BasicTypes.insert {
            it[id] = 1
            it[shortCol] = 7
            it[intCol] = 42
            it[longCol] = 1000L
            it[boolCol] = true
            it[floatCol] = 1.5f
            it[doubleCol] = 2.5
            it[varcharCol] = "hello"
            it[textCol] = "world"
        }

        val row = BasicTypes.selectAll().single()

        assertEquals(7.toShort(), row[BasicTypes.shortCol])
        assertEquals(42, row[BasicTypes.intCol])
        assertEquals(1000L, row[BasicTypes.longCol])
        assertEquals(true, row[BasicTypes.boolCol])
        assertEquals(1.5f, row[BasicTypes.floatCol])
        assertEquals(2.5, row[BasicTypes.doubleCol])
        assertEquals("hello", row[BasicTypes.varcharCol])
        assertEquals("world", row[BasicTypes.textCol])
    }

    @Test
    fun `should generate expected ddl for basic types`() = tx {
        val ddl = BasicTypes.ddl.joinToString(" ")

        assertTrue(ddl.contains("short_col Int16"))
        assertTrue(ddl.contains("int_col Int32"))
        assertTrue(ddl.contains("long_col Int64"))
        assertTrue(ddl.contains("bool_col Bool"))
        assertTrue(ddl.contains("float_col Float"))
        assertTrue(ddl.contains("double_col Double"))
        assertTrue(ddl.contains("varchar_col Utf8"))
        assertTrue(ddl.contains("text_col Utf8"))
        assertTrue(ddl.contains("PRIMARY KEY (id)"))
    }
}

//package tech.ydb.exposed.dialect.integration.types
//
//import org.jetbrains.exposed.v1.core.Table
//import org.jetbrains.exposed.v1.jdbc.transactions.transaction
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.Test
//import tech.ydb.exposed.dialect.basic.YdbTable
//
//class TypesIT {
//
//    object TestTable : YdbTable("test_types") {
//        val id = integer("id")
//        val text = varchar("text", 255)
//
//        override val primaryKey = PrimaryKey(id)
//    }
//
//    @Test
//    fun `should map types correctly`() {
//
//        transaction {
//
//            val ddl = TestTable.ddl.joinToString(" ")
//
//            Assertions.assertTrue(ddl.contains("Int32"))
//            Assertions.assertTrue(ddl.contains("Utf8") || ddl.contains("String"))
//        }
//    }
//}