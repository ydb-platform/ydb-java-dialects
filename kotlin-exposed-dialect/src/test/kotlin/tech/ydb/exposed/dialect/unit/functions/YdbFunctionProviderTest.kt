package tech.ydb.exposed.dialect.unit.functions

import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.stringLiteral
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbFunctionProvider
import tech.ydb.exposed.dialect.YdbJsonDocumentStringColumnType
import tech.ydb.exposed.dialect.YdbJsonStringColumnType
import tech.ydb.exposed.dialect.buildYdbJsonPath

class YdbFunctionProviderTest {

    private val provider = YdbFunctionProvider

    private fun sql(build: QueryBuilder.() -> Unit): String =
        QueryBuilder(false).apply(build).toString()

    @Test
    fun `buildYdbJsonPath maps object keys and array indexes`() {
        assertEquals("$", buildYdbJsonPath())
        assertEquals("$.friends[0].name", buildYdbJsonPath("friends", "0", "name"))
        assertEquals("$.title", buildYdbJsonPath("title"))
        assertEquals("$.friends[*]", buildYdbJsonPath("friends", "[*]"))
    }

    @Test
    fun `buildYdbJsonPath quotes keys with special characters`() {
        assertEquals("$.\"key-name\"", buildYdbJsonPath("key-name"))
    }

    @Test
    fun `charLength uses Unicode GetLength`() {
        val expr = stringLiteral("hello")
        val result = sql { provider.charLength(expr, this) }
        assertEquals("Unicode::GetLength('hello')", result)
    }

    @Test
    fun `substring uses Unicode Substring`() {
        val expr = stringLiteral("abcdef")
        val result = sql {
            provider.substring(expr, intLiteral(2), intLiteral(3), this)
        }
        assertEquals("Unicode::Substring('abcdef', 2, 3)", result)
    }

    @Test
    fun `concat without separator uses concatenation operator`() {
        val result = sql {
            provider.concat("", this, stringLiteral("a"), stringLiteral("b"))
        }
        assertEquals("'a' || 'b'", result)
    }

    @Test
    fun `concat with separator uses Unicode JoinFromList`() {
        val result = sql {
            provider.concat("-", this, stringLiteral("a"), stringLiteral("b"))
        }
        assertEquals(
            "Unicode::JoinFromList(AsList(CAST('a' AS Utf8), CAST('b' AS Utf8)), '-')",
            result
        )
    }

    @Test
    fun `locate returns one-based index`() {
        val expr = stringLiteral("abcdef")
        val result = sql { provider.locate(this, expr, "cd") }
        assertEquals(
            "COALESCE(CAST(Unicode::Find('abcdef', 'cd') + 1u AS Int32), 0)",
            result
        )
    }

    @Test
    fun `locate escapes quotes in needle`() {
        val expr = stringLiteral("a'b")
        val result = sql { provider.locate(this, expr, "x'y") }
        assertEquals(
            "COALESCE(CAST(Unicode::Find('a''b', 'x''y') + 1u AS Int32), 0)",
            result
        )
    }

    @Test
    fun `regexp case sensitive uses REGEXP operator`() {
        val haystack = stringLiteral("aaabccc")
        val pattern = stringLiteral("b+")
        val result = sql { provider.regexp(haystack, pattern, caseSensitive = true, this) }
        assertEquals("'aaabccc' REGEXP 'b+'", result)
    }

    @Test
    fun `regexp case insensitive uses Re2 Match`() {
        val haystack = stringLiteral("Foo")
        val pattern = stringLiteral("foo")
        val result = sql { provider.regexp(haystack, pattern, caseSensitive = false, this) }
        assertEquals(
            "Re2::Grep('foo', Re2::Options(false AS CaseSensitive))('Foo')",
            result
        )
    }

    @Test
    fun `jsonCast casts to column sql type`() {
        val expr = stringLiteral("""{"a":1}""")
        val json = sql { provider.jsonCast(expr, YdbJsonStringColumnType(), this) }
        val jsonDocument = sql { provider.jsonCast(expr, YdbJsonDocumentStringColumnType(), this) }

        assertEquals("CAST('{\"a\":1}' AS Json)", json)
        assertEquals("CAST('{\"a\":1}' AS JsonDocument)", jsonDocument)
    }

    @Test
    fun `jsonExtract scalar uses JSON_VALUE`() {
        val expr = stringLiteral("""{"friends":[{"name":"Jim"}]}""")
        val result = sql {
            provider.jsonExtract(
                expr,
                "friends",
                "0",
                "name",
                toScalar = true,
                jsonType = YdbJsonStringColumnType(),
                queryBuilder = this
            )
        }
        assertEquals(
            "JSON_VALUE('{\"friends\":[{\"name\":\"Jim\"}]}', '$.friends[0].name')",
            result
        )
    }

    @Test
    fun `jsonExtract object uses JSON_QUERY`() {
        val expr = stringLiteral("""{"friends":[]}""")
        val result = sql {
            provider.jsonExtract(
                expr,
                "friends",
                toScalar = false,
                jsonType = YdbJsonStringColumnType(),
                queryBuilder = this
            )
        }
        assertEquals("JSON_QUERY('{\"friends\":[]}', '$.friends')", result)
    }

    @Test
    fun `jsonExists uses JSON_EXISTS`() {
        val expr = stringLiteral("""{"title":"Rocinante"}""")
        val result = sql {
            provider.jsonExists(
                expr,
                "title",
                optional = null,
                jsonType = YdbJsonStringColumnType(),
                queryBuilder = this
            )
        }
        assertEquals("JSON_EXISTS('{\"title\":\"Rocinante\"}', '$.title')", result)
    }

    @Test
    fun `jsonExists supports optional ON ERROR clause`() {
        val expr = stringLiteral("{}")
        val result = sql {
            provider.jsonExists(
                expr,
                "missing",
                optional = "ERROR ON ERROR",
                jsonType = YdbJsonStringColumnType(),
                queryBuilder = this
            )
        }
        assertEquals("JSON_EXISTS('{}', '$.missing' ERROR ON ERROR)", result)
    }

    @Test
    fun `jsonContains is not supported`() {
        assertThrows(UnsupportedOperationException::class.java) {
            sql {
                provider.jsonContains(
                    stringLiteral("{}"),
                    stringLiteral("""{"a":1}"""),
                    path = null,
                    jsonType = YdbJsonStringColumnType(),
                    queryBuilder = this
                )
            }
        }
    }

    @Test
    fun `random without seed uses bare Random`() {
        assertEquals("Random()", provider.random(seed = null))
    }

    @Test
    fun `random with seed passes constant for YDB call grouping`() {
        assertEquals("Random(42)", provider.random(seed = 42))
    }

    @Test
    fun `queryLimitAndOffset`() {
        assertEquals(" LIMIT 10", provider.queryLimitAndOffset(size = 10, offset = 0, alreadyOrdered = false))
        assertEquals(" LIMIT 10 OFFSET 5", provider.queryLimitAndOffset(size = 10, offset = 5, alreadyOrdered = false))
        assertEquals(" OFFSET 5", provider.queryLimitAndOffset(size = null, offset = 5, alreadyOrdered = false))
    }
}
