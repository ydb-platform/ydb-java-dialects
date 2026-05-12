package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Table

/**
 * Base class for YDB row-oriented tables.
 *
 * Adds YDB-specific DDL extensions on top of Exposed's [Table] (see [YdbTableDsl]):
 *  - [ttl] declares a TTL on a date/numeric column;
 *  - [secondaryIndex] declares a YDB secondary index with COVER / ASYNC / WITH params.
 *
 * Tables that need a generated primary key for DAO should use [YdbIdTable] (or one of its
 * specializations: [YdbUuidIdTable], [YdbUlidTable], [YdbStringIdTable]).
 */
open class YdbTable(name: String = "") :
    Table(name),
    YdbTableDsl by YdbTableFeatures() {

    override fun createStatement(): List<String> =
        buildYdbCreateStatement(this, ttlSettings, ydbSecondaryIndices)
}
