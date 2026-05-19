/**
 * YDB secondary index declarations for [YdbTable.secondaryIndex].
 *
 * Rendered into `CREATE TABLE` as `INDEX name GLOBAL [UNIQUE] [ASYNC] ON (...) [COVER (...)] [WITH (...)]`.
 */
package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

/** Index visibility scope in YQL (`GLOBAL` for row-oriented tables). */
enum class YdbIndexScope {
    GLOBAL
}

/** Whether the index is built synchronously with writes or in the background. */
enum class YdbIndexSyncMode {
    SYNC,
    ASYNC
}

/** Internal model produced by [YdbTable.secondaryIndex] and rendered by [renderYdbSecondaryIndex]. */
data class YdbSecondaryIndexSpec(
    val name: String,
    val columns: List<Column<*>>,
    val unique: Boolean = false,
    val scope: YdbIndexScope = YdbIndexScope.GLOBAL,
    val syncMode: YdbIndexSyncMode = YdbIndexSyncMode.SYNC,
    val indexType: String? = null,
    val coverColumns: List<Column<*>> = emptyList(),
    val withParams: Map<String, Any> = emptyMap()
)

/** Builds the `INDEX ...` fragment for a single secondary index inside `CREATE TABLE`. */
internal fun renderYdbSecondaryIndex(spec: YdbSecondaryIndexSpec): String {
    val tr = TransactionManager.current()

    require(spec.columns.isNotEmpty()) {
        "YDB secondary index must contain at least one column"
    }

    require(spec.name.isNotBlank()) {
        "YDB secondary index name must not be blank"
    }

    require(spec.scope == YdbIndexScope.GLOBAL) {
        "Only GLOBAL secondary indexes are supported by YDB row-oriented tables in this dialect"
    }

    val indexName = tr.db.identifierManager.cutIfNecessaryAndQuote(spec.name)

    val columnsSql = spec.columns.joinToString(", ") { tr.identity(it) }
    val coverSql = spec.coverColumns
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { tr.identity(it) }

    val withSql = spec.withParams
        .takeIf { it.isNotEmpty() }
        ?.entries
        ?.joinToString(", ") { (k, v) -> "$k = ${renderYdbIndexParamValue(v)}" }

    return buildString {
        append("INDEX ")
        append(indexName)
        append(" ")
        append(spec.scope.name)

        if (spec.unique) {
            append(" UNIQUE")
        }

        if (spec.syncMode != YdbIndexSyncMode.SYNC) {
            append(" ")
            append(spec.syncMode.name)
        }

        if (spec.indexType != null) {
            append(" USING ")
            append(spec.indexType)
        }

        append(" ON (")
        append(columnsSql)
        append(")")

        if (coverSql != null) {
            append(" COVER (")
            append(coverSql)
            append(")")
        }

        if (withSql != null) {
            append(" WITH (")
            append(withSql)
            append(")")
        }
    }
}

private fun renderYdbIndexParamValue(value: Any): String = when (value) {
    is Number -> value.toString()
    is Boolean -> value.toString().uppercase()
    else -> "\"${value.toString().replace("\"", "\\\"")}\""
}
