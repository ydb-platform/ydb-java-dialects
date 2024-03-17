package tech.ydb.data.core.convert;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ClassUtils;

/**
 * @author Madiyar Nurgazin
 */
public enum YdbJdbcColumnTypes {
    INSTANCE {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Class<?> resolvePrimitiveType(Class<?> type) {

            return javaToDbType.entrySet().stream() //
                    .filter(e -> e.getKey().isAssignableFrom(type)) //
                    .map(e -> (Class<?>) e.getValue()) //
                    .findFirst() //
                    .orElseGet(() -> (Class) ClassUtils.resolvePrimitiveIfNecessary(type));
        }
    };

    private static final Map<Class<?>, Class<?>> javaToDbType = new LinkedHashMap<>();

    static {

        javaToDbType.put(Enum.class, String.class);
        javaToDbType.put(ZonedDateTime.class, String.class);
        javaToDbType.put(OffsetDateTime.class, OffsetDateTime.class);
        javaToDbType.put(LocalDateTime.class, Time.class);
        javaToDbType.put(LocalDate.class, Date.class);
        javaToDbType.put(Temporal.class, Timestamp.class);
    }

    public abstract Class<?> resolvePrimitiveType(Class<?> type);
}
