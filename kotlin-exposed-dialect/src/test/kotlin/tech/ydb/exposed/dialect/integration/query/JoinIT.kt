package tech.ydb.exposed.dialect.integration.query

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class JoinIT : BaseYdbTest() {

    object Authors : YdbTable("authors") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    object Books : YdbTable("books") {
        val id = integer("id")
        val title = varchar("title", 255)
        val authorId = integer("author_id")
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Authors, Books)

    @Test
    fun `should support inner join`() = tx {
        Authors.insert {
            it[id] = 1
            it[name] = "Alice"
        }
        Authors.insert {
            it[id] = 2
            it[name] = "Bob"
        }

        Books.insert {
            it[id] = 10
            it[title] = "A-Book"
            it[authorId] = 1
        }
        Books.insert {
            it[id] = 11
            it[title] = "B-Book"
            it[authorId] = 2
        }

        val rows = Authors
            .join(
                otherTable = Books,
                joinType = JoinType.INNER,
                onColumn = Authors.id,
                otherColumn = Books.authorId
            )
            .select(Authors.name, Books.title)
            .orderBy(Books.id to SortOrder.ASC)
            .toList()

        assertEquals(2, rows.size)
        assertEquals("Alice", rows[0][Authors.name])
        assertEquals("A-Book", rows[0][Books.title])
        assertEquals("Bob", rows[1][Authors.name])
        assertEquals("B-Book", rows[1][Books.title])
    }

    @Test
    fun `should support filtered join query`() = tx {
        Authors.insert {
            it[id] = 1
            it[name] = "Alice"
        }
        Authors.insert {
            it[id] = 2
            it[name] = "Bob"
        }

        Books.insert {
            it[id] = 10
            it[title] = "A-Book"
            it[authorId] = 1
        }
        Books.insert {
            it[id] = 11
            it[title] = "B-Book"
            it[authorId] = 2
        }

        val rows = Authors
            .join(
                otherTable = Books,
                joinType = JoinType.INNER,
                onColumn = Authors.id,
                otherColumn = Books.authorId
            )
            .select(Authors.name, Books.title)
            .where { Authors.name eq "Alice" }
            .toList()

        assertEquals(1, rows.size)
        assertEquals("Alice", rows.single()[Authors.name])
        assertEquals("A-Book", rows.single()[Books.title])
    }
}