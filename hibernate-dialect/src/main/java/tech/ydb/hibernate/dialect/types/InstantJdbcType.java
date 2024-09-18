package tech.ydb.hibernate.dialect.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsInstantJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Kirill Kurdyukov
 */
public class InstantJdbcType extends TimestampUtcAsInstantJdbcType {

    @Override
    public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(Integer length, Integer scale, TypeConfiguration typeConfiguration) {
        return typeConfiguration.getJavaTypeRegistry().getDescriptor(Instant.class);
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(
                    PreparedStatement st,
                    X value,
                    int index,
                    WrapperOptions wrapperOptions) throws SQLException {
                final Instant instant = javaType.unwrap(value, Instant.class, wrapperOptions);

                st.setObject(index, instant, Types.TIMESTAMP);
            }

            @Override
            protected void doBind(
                    CallableStatement st,
                    X value,
                    String name,
                    WrapperOptions wrapperOptions)
                    throws SQLException {
                final Instant instant = javaType.unwrap(value, Instant.class, wrapperOptions);

                st.setObject(name, instant, Types.TIMESTAMP);
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
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
