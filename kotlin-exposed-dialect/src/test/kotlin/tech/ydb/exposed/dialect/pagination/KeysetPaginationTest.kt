package tech.ydb.exposed.dialect.pagination


import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

object TestUsers : Table("users") {
    val id = integer("id")
    val name = varchar("name", 255)
}

class KeysetPaginationTest {

    @Test
    fun `should apply keyset pagination`() {

        val query = TestUsers
            .selectAll()
            .keysetPage(TestUsers.id, lastValue = 10, limit = 20)

        val sql = query.prepareSQL(QueryBuilder(false))

        assertTrue(sql.contains("WHERE"))
        assertTrue(sql.contains("id"))
        assertTrue(sql.contains("LIMIT"))
    }

    @Test
    fun `should generate first page without where`() {

        val query = TestUsers
            .selectAll()
            .keysetPage(TestUsers.id, lastValue = null, limit = 20)

        val sql = query.prepareSQL(QueryBuilder(false))

        assertTrue(sql.contains("LIMIT"))
    }
}

