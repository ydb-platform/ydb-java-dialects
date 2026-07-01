package tech.ydb.hibernate.dialect.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import tech.ydb.hibernate.dialect.code.YdbJdbcCode;

/**
 * @author Kirill Kurdyukov
 */
public class LocalDateTimeJdbcType extends TimestampJdbcType {

    public static final LocalDateTimeJdbcType INSTANCE = new LocalDateTimeJdbcType();

    @Override
    public String toString() {
        return "LocalDateTimeTypeDescriptor";
    }

    @Override
    public int getJdbcTypeCode() {
        return YdbJdbcCode.DATETIME;
    }

    @Override
    public String getFriendlyName() {
        return "Datetime";
    }

    @Override
    public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
        return LocalDateTime.class;
    }

    @Override
    public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(Integer length, Integer scale,
                                                             TypeConfiguration typeConfiguration) {
        return typeConfiguration.getJavaTypeRegistry().getDescriptor(LocalDateTime.class);
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                final LocalDateTime localDateTime = javaType.unwrap(value, LocalDateTime.class, options);

                st.setObject(index, localDateTime, YdbJdbcCode.DATETIME);
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
                final LocalDateTime localDateTime = javaType.unwrap(value, LocalDateTime.class, options);

                st.setObject(name, localDateTime, YdbJdbcCode.DATETIME);
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new ValueExtractor<>() {
            @Override
            public X extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
                return javaType.wrap(rs.getObject(paramIndex), options);
            }

            @Override
            public X extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
                return javaType.wrap(statement.getObject(paramIndex), options);
            }

            @Override
            public X extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
                return javaType.wrap(statement.getObject(paramName), options);
            }
        };
    }
}
