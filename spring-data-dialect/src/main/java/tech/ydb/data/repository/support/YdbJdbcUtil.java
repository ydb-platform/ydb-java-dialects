package tech.ydb.data.repository.support;

import java.sql.JDBCType;
import java.sql.SQLType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.jdbc.support.JdbcUtil;
import org.springframework.util.Assert;
import tech.ydb.data.core.convert.YQLType;
import tech.ydb.jdbc.YdbConst;
import tech.ydb.table.values.PrimitiveType;

/**
 * @author Madiyar Nurgazin
 */
public final class YdbJdbcUtil {
    private static final Map<Class<?>, SQLType> sqlTypeByClass = new HashMap<>();
    private static final Map<PrimitiveType, SQLType> sqlTypeByYdbPrimitiveType = new HashMap<>();

    static {
        sqlTypeByClass.put(LocalDateTime.class, JDBCType.TIME);
        sqlTypeByClass.put(LocalDate.class, JDBCType.DATE);
        sqlTypeByClass.put(Instant.class, JDBCType.TIMESTAMP);

        sqlTypeByYdbPrimitiveType.put(PrimitiveType.Json, bindPrimitive(PrimitiveType.Json));
        sqlTypeByYdbPrimitiveType.put(PrimitiveType.Yson, bindPrimitive(PrimitiveType.Yson));
        sqlTypeByYdbPrimitiveType.put(PrimitiveType.JsonDocument, bindPrimitive(PrimitiveType.JsonDocument));
        sqlTypeByYdbPrimitiveType.put(PrimitiveType.Uuid, bindPrimitive(PrimitiveType.Uuid));
        sqlTypeByYdbPrimitiveType.put(PrimitiveType.Uint8, bindPrimitive(PrimitiveType.Uint8));
        sqlTypeByYdbPrimitiveType.put(PrimitiveType.Uint16, bindPrimitive(PrimitiveType.Uint16));
        sqlTypeByYdbPrimitiveType.put(PrimitiveType.Uint32, bindPrimitive(PrimitiveType.Uint32));
        sqlTypeByYdbPrimitiveType.put(PrimitiveType.Uint64, bindPrimitive(PrimitiveType.Uint64));
    }

    private YdbJdbcUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static SQLType targetSqlTypeFor(Class<?> type) {
        Assert.notNull(type, "Type must not be null");

        return sqlTypeByClass.keySet().stream() //
                .filter(k -> k.isAssignableFrom(type)) //
                .findFirst() //
                .map(sqlTypeByClass::get) //
                .orElse(JdbcUtil.targetSqlTypeFor(type));
    }

    public static SQLType targetSqlTypeFor(PrimitiveType type) {
        Assert.notNull(type, "Type must not be null");

        return sqlTypeByYdbPrimitiveType.getOrDefault(type, JdbcUtil.TYPE_UNKNOWN);
    }

    private static YQLType bindPrimitive(PrimitiveType type) {
        return new YQLType(type.name(), YdbConst.SQL_KIND_PRIMITIVE + type.ordinal());
    }
}
