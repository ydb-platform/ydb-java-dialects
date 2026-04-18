package tech.ydb.exposed.dialect.integration.pagination

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.pagination.keysetPageAsc
import tech.ydb.exposed.dialect.pagination.keysetPageDesc

class KeysetPaginationIT : BaseYdbTest() {

    object FeedItems : YdbTable("feed_items") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(FeedItems)

    @Test
    fun `should support forward keyset pagination`() = tx {
        (1..5).forEach { i ->
            FeedItems.insert {
                it[id] = i
                it[name] = "item-$i"
            }
        }

        val page1 = FeedItems
            .selectAll()
            .keysetPageAsc(FeedItems.id, lastValue = null, limit = 2)
            .toList()

        assertEquals(2, page1.size)
        assertEquals(1, page1[0][FeedItems.id])
        assertEquals(2, page1[1][FeedItems.id])

        val lastSeenId = page1.last()[FeedItems.id]

        val page2 = FeedItems
            .selectAll()
            .keysetPageAsc(FeedItems.id, lastValue = lastSeenId, limit = 2)
            .toList()

        assertEquals(2, page2.size)
        assertEquals(3, page2[0][FeedItems.id])
        assertEquals(4, page2[1][FeedItems.id])

        val page3 = FeedItems
            .selectAll()
            .keysetPageAsc(FeedItems.id, lastValue = page2.last()[FeedItems.id], limit = 2)
            .toList()

        assertEquals(1, page3.size)
        assertEquals(5, page3[0][FeedItems.id])
    }

    @Test
    fun `should support backward keyset pagination`() = tx {
        (1..5).forEach { i ->
            FeedItems.insert {
                it[id] = i
                it[name] = "item-$i"
            }
        }

        val page1 = FeedItems
            .selectAll()
            .keysetPageDesc(FeedItems.id, lastValue = null, limit = 2)
            .toList()

        assertEquals(2, page1.size)
        assertEquals(5, page1[0][FeedItems.id])
        assertEquals(4, page1[1][FeedItems.id])

        val lastSeenId = page1.last()[FeedItems.id]

        val page2 = FeedItems
            .selectAll()
            .keysetPageDesc(FeedItems.id, lastValue = lastSeenId, limit = 2)
            .toList()

        assertEquals(2, page2.size)
        assertEquals(3, page2[0][FeedItems.id])
        assertEquals(2, page2[1][FeedItems.id])
    }
}