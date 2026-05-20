package tech.ydb.exposed.dialect.integration.dao

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

/** Exposed [Entity] / [IdTable] smoke test against YDB. */
class DaoSmokeIT : BaseYdbTest() {

    object Articles : IdTable<String>("dao_articles") {
        override val id = varchar("id", 64).entityId()
        val title = varchar("title", 255)
        val body = text("body")

        override val primaryKey = PrimaryKey(id)

        override fun createStatement(): List<String> = createYdbStatement()
    }

    class Article(id: EntityID<String>) : Entity<String>(id) {
        companion object : EntityClass<String, Article>(Articles)

        var title by Articles.title
        var body by Articles.body
    }

    override val tables: List<Table> = listOf(Articles)

    @Test
    fun `should support dao create read update delete with manual string id`() {
        tx {
            Article.new(id = "article-1") {
                title = "draft"
                body = "hello"
            }
        }

        tx {
            val loaded = Article.findById("article-1")
            assertNotNull(loaded)
            assertEquals("draft", loaded!!.title)
            assertEquals("hello", loaded.body)

            loaded.title = "published"
        }

        tx {
            val reloaded = Article.findById("article-1")
            assertNotNull(reloaded)
            assertEquals("published", reloaded!!.title)

            reloaded.delete()
        }

        tx {
            val deleted = Article.findById("article-1")
            assertNull(deleted)
            assertEquals(0, Articles.selectAll().count())
        }
    }
}
