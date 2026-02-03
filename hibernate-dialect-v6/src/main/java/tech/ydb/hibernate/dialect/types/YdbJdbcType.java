package tech.ydb.hibernate.dialect.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Kirill Kurdyukov
 */
public class YdbJdbcType implements JdbcType {

    private final int jdbcTypeCode;
    private final Class<?> classToken;

    public YdbJdbcType(int jdbcTypeCode, Class<?> classToken) {
        this.jdbcTypeCode = jdbcTypeCode;
        this.classToken = classToken;
    }

    @Override
    public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
            Integer precision,
            Integer scale,
            TypeConfiguration typeConfiguration) {
        return typeConfiguration.getJavaTypeRegistry().getDescriptor(classToken);
    }

    @Override
    public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
        return classToken;
    }

    @Override
    public int getJdbcTypeCode() {
        return jdbcTypeCode;
    }

    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new ValueBinder<>() {
            @Override
            public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                st.setObject(index, javaType.unwrap(value, classToken, options), getJdbcTypeCode());
            }

            @Override
            public void bind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
                st.setObject(name, javaType.unwrap(value, classToken, options), getJdbcTypeCode());
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
