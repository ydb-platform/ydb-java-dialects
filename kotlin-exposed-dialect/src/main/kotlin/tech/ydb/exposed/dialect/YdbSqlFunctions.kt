package tech.ydb.exposed.dialect

/**
 * Builds a [JsonPath](https://ydb.tech/docs/en/yql/reference/builtins/json) query for YDB
 * `JSON_VALUE` / `JSON_QUERY` / `JSON_EXISTS` from Exposed path segments.
 *
 * Segments are usually object keys; all-digit segments become array indexes (`[0]`).
 * A single segment that already starts with `$` is returned as-is.
 */
internal fun buildYdbJsonPath(vararg segments: String): String {
    if (segments.isEmpty()) return "$"
    if (segments.size == 1) {
        val only = segments[0]
        if (only.startsWith("$")) return only
    }

    val path = StringBuilder("$")
    for (segment in segments) {
        if (segment.isEmpty()) continue
        when {
            segment.all(Char::isDigit) -> path.append('[').append(segment).append(']')
            segment.startsWith("[") && segment.endsWith("]") -> path.append(segment)
            else -> {
                if (path.last() == '$' || path.last() == ']') {
                    path.append('.')
                }
                path.append(quoteJsonPathKey(segment))
            }
        }
    }
    return path.toString()
}

private fun quoteJsonPathKey(key: String): String =
    if (key.all { it.isLetterOrDigit() || it == '_' }) {
        key
    } else {
        "\"${key.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }

internal fun escapeYqlStringLiteral(value: String): String =
    value.replace("'", "''")

internal fun escapeYqlDoubleQuotedLiteral(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")
