package tech.ydb.hibernate.dialect.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Kirill Kurdyukov
 */
public class LocalDateJdbcType extends DateJdbcType {

    public static final LocalDateJdbcType INSTANCE = new LocalDateJdbcType();

    @Override
    public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
        return LocalDate.class;
    }

    @Override
    public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(Integer length, Integer scale,
                                                             TypeConfiguration typeConfiguration) {
        return typeConfiguration.getJavaTypeRegistry().getDescriptor(LocalDate.class);
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                final LocalDate date = javaType.unwrap(value, LocalDate.class, options);

                st.setObject(index, date, Types.DATE);
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                final LocalDate date = javaType.unwrap(value, LocalDate.class, options);

                st.setObject(name, date, Types.DATE);
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
