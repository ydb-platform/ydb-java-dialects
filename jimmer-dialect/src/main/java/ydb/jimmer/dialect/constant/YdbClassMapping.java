package ydb.jimmer.dialect.constant;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provides mappings from Java classes
 * to the YDB and JDBC data types.
 */
public final class YdbClassMapping {
    private YdbClassMapping() {}

    public static final Map<Class<?>, String> classToYdbType;
    public static final Map<Class<?>, Integer> classToJdbcType;

    private static final Map<Class<?>, String> classToYdbTypeBuilder = new HashMap<>();
    private static final Map<Class<?>, Integer> classToJdbcTypeBuilder = new HashMap<>();

    static {
        add("Bool", YdbJdbcTypes.BOOL, boolean.class, Boolean.class);

        add("Int8", YdbJdbcTypes.INT8, byte.class, Byte.class);
        add("Int16", YdbJdbcTypes.INT16, short.class, Short.class);
        add("Int32", YdbJdbcTypes.INT32, int.class, Integer.class, LocalTime.class, Time.class);
        add("Int64", YdbJdbcTypes.INT64, long.class, Long.class, BigInteger.class);
        add("Decimal(22, 9)", YdbJdbcTypes.DECIMAL_22_9, BigDecimal.class);

        add("Float", YdbJdbcTypes.FLOAT, float.class, Float.class);
        add("Double", YdbJdbcTypes.DOUBLE, double.class, Double.class);

        add("Utf8", YdbJdbcTypes.TEXT, String.class);
        add("String", YdbJdbcTypes.BYTES, byte[].class);

        add("Uuid", YdbJdbcTypes.UUID, UUID.class);

        add("Date32", YdbJdbcTypes.DATE32, Date.class, LocalDate.class);
        add("Datetime64", YdbJdbcTypes.DATETIME64, LocalDateTime.class);
        add("Timestamp64", YdbJdbcTypes.TIMESTAMP64, java.util.Date.class, Timestamp.class, Instant.class);
        add("Interval64", YdbJdbcTypes.INTERVAL64, Duration.class);

        classToYdbType = Map.copyOf(classToYdbTypeBuilder);
        classToJdbcType = Map.copyOf(classToJdbcTypeBuilder);
    }

    private static void add(String ydbType, int jdbcType, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            classToYdbTypeBuilder.put(clazz, ydbType);
            classToJdbcTypeBuilder.put(clazz, jdbcType);
        }
    }
}
