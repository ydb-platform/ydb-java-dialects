package tech.ydb.exposed.dialect.code

/**
 * YDB JDBC vendor type codes for [java.sql.PreparedStatement.setObject] (index, value, sqlType).
 *
 * Layout matches the YDB JDBC driver ([YdbTypes](https://github.com/ydb-platform/ydb-jdbc-driver/blob/master/jdbc/src/main/java/tech/ydb/jdbc/common/YdbTypes.java)).
 */
internal object YdbJdbcCode {
    const val SQL_KIND_PRIMITIVE: Int = 10_000
    const val SQL_KIND_DECIMAL: Int = 1 shl 14

    const val BOOL: Int = SQL_KIND_PRIMITIVE
    const val INT8: Int = SQL_KIND_PRIMITIVE + 1
    const val UINT8: Int = SQL_KIND_PRIMITIVE + 2
    const val INT16: Int = SQL_KIND_PRIMITIVE + 3
    const val UINT16: Int = SQL_KIND_PRIMITIVE + 4
    const val INT32: Int = SQL_KIND_PRIMITIVE + 5
    const val UINT32: Int = SQL_KIND_PRIMITIVE + 6
    const val INT64: Int = SQL_KIND_PRIMITIVE + 7
    const val UINT64: Int = SQL_KIND_PRIMITIVE + 8
    const val FLOAT: Int = SQL_KIND_PRIMITIVE + 9
    const val DOUBLE: Int = SQL_KIND_PRIMITIVE + 10
    const val BYTES: Int = SQL_KIND_PRIMITIVE + 11
    const val TEXT: Int = SQL_KIND_PRIMITIVE + 12
    const val YSON: Int = SQL_KIND_PRIMITIVE + 13
    const val JSON: Int = SQL_KIND_PRIMITIVE + 14
    const val UUID: Int = SQL_KIND_PRIMITIVE + 15
    const val DATE: Int = SQL_KIND_PRIMITIVE + 16
    const val DATETIME: Int = SQL_KIND_PRIMITIVE + 17
    const val TIMESTAMP: Int = SQL_KIND_PRIMITIVE + 18
    const val INTERVAL: Int = SQL_KIND_PRIMITIVE + 19
    const val TZ_DATE: Int = SQL_KIND_PRIMITIVE + 20
    const val TZ_DATETIME: Int = SQL_KIND_PRIMITIVE + 21
    const val TZ_TIMESTAMP: Int = SQL_KIND_PRIMITIVE + 22
    const val JSON_DOCUMENT: Int = SQL_KIND_PRIMITIVE + 23
    const val DATE32: Int = SQL_KIND_PRIMITIVE + 25
    const val DATETIME64: Int = SQL_KIND_PRIMITIVE + 26
    const val TIMESTAMP64: Int = SQL_KIND_PRIMITIVE + 27
    const val INTERVAL64: Int = SQL_KIND_PRIMITIVE + 28

    fun decimal(precision: Int, scale: Int): Int {
        require(precision in 1..35) { "YDB Decimal precision must be in 1..35" }
        require(scale in 0..precision) { "YDB Decimal scale must be in 0..precision" }
        return SQL_KIND_DECIMAL + (precision shl 6) + scale
    }
}
