package tech.ydb.data.support;

import java.sql.JDBCType;
import java.sql.SQLType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.jdbc.support.JdbcUtil;
import org.springframework.util.Assert;

/**
 * @author Madiyar Nurgazin
 */
public final class YdbJdbcUtil {
    private static final Map<Class<?>, SQLType> sqlTypeMappings = new HashMap<>();

    static {
        sqlTypeMappings.put(LocalDateTime.class, JDBCType.TIME);
        sqlTypeMappings.put(LocalDate.class, JDBCType.DATE);
        sqlTypeMappings.put(Instant.class, JDBCType.TIMESTAMP);
    }

    private YdbJdbcUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static SQLType targetSqlTypeFor(Class<?> type) {
        Assert.notNull(type, "Type must not be null");

        return sqlTypeMappings.keySet().stream() //
                .filter(k -> k.isAssignableFrom(type)) //
                .findFirst() //
                .map(sqlTypeMappings::get) //
                .orElse(JdbcUtil.targetSqlTypeFor(type));
    }
}
