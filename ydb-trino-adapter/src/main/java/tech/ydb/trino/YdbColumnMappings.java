package tech.ydb.trino;

import io.trino.plugin.jdbc.ColumnMapping;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.LongWriteFunction;
import io.trino.spi.TrinoException;

import java.util.Locale;

import static io.trino.plugin.jdbc.StandardColumnMappings.dateReadFunctionUsingLocalDate;
import static io.trino.plugin.jdbc.StandardColumnMappings.fromTrinoTimestamp;
import static io.trino.plugin.jdbc.StandardColumnMappings.timestampReadFunction;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static io.trino.spi.type.DateType.DATE;
import static io.trino.spi.type.TimestampType.TIMESTAMP_MICROS;
import static java.time.ZoneOffset.UTC;
import static tech.ydb.jdbc.YdbConst.SQL_KIND_PRIMITIVE;

final class YdbColumnMappings {
    // https://github.com/ydb-platform/ydb-jdbc-driver/blob/v2.4.1/jdbc/src/main/java/tech/ydb/jdbc/common/YdbTypes.java#L64-L77
    private static final int YDB_DATE_SQL_TYPE = SQL_KIND_PRIMITIVE + 16;
    private static final int YDB_DATETIME_SQL_TYPE = SQL_KIND_PRIMITIVE + 17;
    private static final int YDB_TIMESTAMP_SQL_TYPE = SQL_KIND_PRIMITIVE + 18;
    static final int YDB_DATE32_SQL_TYPE = SQL_KIND_PRIMITIVE + 25;
    private static final int YDB_DATETIME64_SQL_TYPE = SQL_KIND_PRIMITIVE + 26;
    static final int YDB_TIMESTAMP64_SQL_TYPE = SQL_KIND_PRIMITIVE + 27;

    private YdbColumnMappings() {}

    static ColumnMapping dateColumnMapping(JdbcTypeHandle typeHandle) {
        String typeName = typeHandle.jdbcTypeName().orElse("");
        LongWriteFunction writeFunction = switch (typeName.toLowerCase(Locale.ROOT)) {
            case "date" -> dateWriteFunction(YDB_DATE_SQL_TYPE);
            case "date32" -> dateWriteFunction(YDB_DATE32_SQL_TYPE);
            default -> throw new TrinoException(NOT_SUPPORTED, "Unsupported YDB date type: " + typeName);
        };
        return ColumnMapping.longMapping(
                DATE,
                dateReadFunctionUsingLocalDate(),
                writeFunction);
    }

    static ColumnMapping timestampColumnMapping(JdbcTypeHandle typeHandle) {
        String typeName = typeHandle.jdbcTypeName().orElse("");
        LongWriteFunction writeFunction = switch (typeName.toLowerCase(Locale.ROOT)) {
            case "datetime" -> datetimeWriteFunction(YDB_DATETIME_SQL_TYPE);
            case "datetime64" -> datetimeWriteFunction(YDB_DATETIME64_SQL_TYPE);
            case "timestamp" -> timestampWriteFunction(YDB_TIMESTAMP_SQL_TYPE);
            case "timestamp64" -> timestampWriteFunction(YDB_TIMESTAMP64_SQL_TYPE);
            default -> throw new TrinoException(NOT_SUPPORTED, "Unsupported YDB timestamp type: " + typeName);
        };
        return ColumnMapping.longMapping(
                TIMESTAMP_MICROS,
                timestampReadFunction(TIMESTAMP_MICROS),
                writeFunction);
    }

    static LongWriteFunction dateWriteFunction(int sqlType) {
        return LongWriteFunction.of(
                sqlType,
                (statement, index, value) -> statement.setObject(index, value, sqlType));
    }

    private static LongWriteFunction datetimeWriteFunction(int sqlType) {
        return LongWriteFunction.of(
                sqlType,
                (statement, index, value) -> statement.setObject(
                        index,
                        fromTrinoTimestamp(value).toEpochSecond(UTC),
                        sqlType));
    }

    static LongWriteFunction timestampWriteFunction(int sqlType) {
        return LongWriteFunction.of(
                sqlType,
                (statement, index, value) -> statement.setObject(
                        index,
                        fromTrinoTimestamp(value).toInstant(UTC),
                        sqlType));
    }
}
