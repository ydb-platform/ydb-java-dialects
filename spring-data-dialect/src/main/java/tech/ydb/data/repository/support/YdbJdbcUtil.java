package tech.ydb.data.repository.support;

import java.sql.JDBCType;
import java.sql.SQLType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.jdbc.support.JdbcUtil;
import org.springframework.util.Assert;

/**
 * @author Madiyar Nurgazin
 */
public final class YdbJdbcUtil {
    private static final Map<Class<?>, SQLType> sqlTypeByClass = new HashMap<>();

    static {
        sqlTypeByClass.put(LocalDate.class, JDBCType.DATE);
        sqlTypeByClass.put(Instant.class, JDBCType.TIMESTAMP);
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
}
