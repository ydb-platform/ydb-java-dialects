package tech.ydb.exposed.dialect.integration.functions

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.charLength
import org.jetbrains.exposed.v1.core.concat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.locate
import org.jetbrains.exposed.v1.core.regexp
import org.jetbrains.exposed.v1.core.substring
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.json.Extract
import org.jetbrains.exposed.v1.json.exists
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbJson

/**
 * Integration tests for [tech.ydb.exposed.dialect.YdbFunctionProvider] string and JSON mappings
 * against a live YDB instance.
 *
 * String functions use Exposed core DSL ([charLength], [substring], …).
 * JSON functions use [exposed-json](https://www.jetbrains.com/help/exposed/json-and-jsonb-types.html)
 * ([extract], [exists]), which delegate to [org.jetbrains.exposed.v1.core.vendors.FunctionProvider].
 */
class YdbSqlFunctionsIT : BaseYdbTest() {

    object Strings : YdbTable("fn_strings") {
        val id = integer("id")
        val value = text("value")

        override val primaryKey = PrimaryKey(id)
    }

    object JsonRows : YdbTable("fn_json") {
        val id = integer("id")
        val payload = ydbJson("payload")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Strings, JsonRows)

    private val sampleJson = """
        {
            "title": "Rocinante",
            "crew": ["James Holden", "Naomi Nagata"],
            "meta": {"active": true}
        }
    """.trimIndent().replace("\n", "").replace(" ", "")

    @Test
    fun `charLength counts Unicode code points`() = tx {
        Strings.insert {
            it[id] = 1
            it[value] = "жніўня"
        }

        val length = Strings.value.charLength()
        assertEquals(6, Strings.select(length).single()[length])
    }

    @Test
    fun `substring extracts by Unicode positions`() = tx {
        Strings.insert {
            it[id] = 1
            it[value] = "0123456789abcdefghij"
        }

        val part = Strings.value.substring(10, 5)
        assertEquals("abcde", Strings.select(part).single()[part])
    }

    @Test
    fun `locate is one-based and returns zero when not found`() = tx {
        Strings.insert {
            it[id] = 1
            it[value] = "abcdef"
        }
        Strings.insert {
            it[id] = 2
            it[value] = "xyz"
        }

        val position = Strings.value.locate("cd")
        assertEquals(3, Strings.select(position).where { Strings.id eq 1 }.single()[position])
        assertEquals(0, Strings.select(position).where { Strings.id eq 2 }.single()[position])
    }

    @Test
    fun `concat joins expressions`() = tx {
        Strings.insert {
            it[id] = 1
            it[value] = "ab"
        }

        val joined = concat("-", listOf(Strings.value, org.jetbrains.exposed.v1.core.stringLiteral("z")))
        assertEquals("ab-z", Strings.select(joined).single()[joined])
    }

    @Test
    fun `regexp matches with REGEXP operator`() = tx {
        Strings.insert {
            it[id] = 1
            it[value] = "aaabccc"
        }
        Strings.insert {
            it[id] = 2
            it[value] = "zzz"
        }

        assertEquals(1, Strings.selectAll().where { Strings.value regexp "b+" }.count())
    }

    @Test
    fun `regexp ignores case via Re2 Grep`() = tx {
        Strings.insert {
            it[id] = 1
            it[value] = "FooBar"
        }

        assertEquals(
            1,
            Strings.selectAll().where {
                Strings.value.regexp(
                    org.jetbrains.exposed.v1.core.stringParam("foo"),
                    caseSensitive = false
                )
            }.count()
        )
    }

    @Test
    fun `extract scalar uses JSON_VALUE via dialect`() = tx {
        JsonRows.insert {
            it[id] = 1
            it[payload] = sampleJson
        }

        val title = jsonExtract("title", toScalar = true)
        assertEquals("Rocinante", JsonRows.select(title).single()[title])
    }

    @Test
    fun `exists uses JSON_EXISTS via dialect`() = tx {
        JsonRows.insert {
            it[id] = 1
            it[payload] = sampleJson
        }

        assertEquals(1, JsonRows.selectAll().where { JsonRows.payload.exists("title") }.count())
        assertEquals(1, JsonRows.selectAll().where { JsonRows.payload.exists("crew", "[*]") }.count())
        assertEquals(0, JsonRows.selectAll().where { JsonRows.payload.exists("missing") }.count())
    }

    @Test
    fun `extract reads array element by index`() = tx {
        JsonRows.insert {
            it[id] = 1
            it[payload] = sampleJson
        }

        val firstCrew = jsonExtract("crew", "0", toScalar = true)
        // JSON_VALUE returns Utf8; YDB may normalize string scalars (e.g. drop spaces in names).
        assertEquals("JamesHolden", JsonRows.select(firstCrew).single()[firstCrew])
    }

    @Test
    fun `extract object uses JSON_QUERY via dialect`() = tx {
        JsonRows.insert {
            it[id] = 1
            it[payload] = sampleJson
        }

        val crew = jsonExtract("crew", toScalar = false)
        val fragment = JsonRows.select(crew).single()[crew]
        assertTrue(!fragment.isNullOrBlank(), "JSON_QUERY crew: $fragment")
        assertTrue(fragment!!.contains("Holden"), "JSON_QUERY crew: $fragment")
    }

    private fun jsonExtract(vararg path: String, toScalar: Boolean) = Extract<String?>(
        JsonRows.payload,
        *path,
        toScalar = toScalar,
        jsonType = JsonRows.payload.columnType,
        columnType = TextColumnType()
    )
}
