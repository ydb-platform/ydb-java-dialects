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
import tech.ydb.hibernate.dialect.code.YdbJdbcCode;

/**
 * @author Kirill Kurdyukov
 */
public class Uint8JdbcType implements JdbcType {
    public static final Uint8JdbcType INSTANCE = new Uint8JdbcType();

    @Override
    public int getJdbcTypeCode() {
        return YdbJdbcCode.UINT8;
    }

    @Override
    public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
            Integer precision,
            Integer scale,
            TypeConfiguration typeConfiguration) {
        return typeConfiguration.getJavaTypeRegistry().getDescriptor(Integer.class);
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new ValueBinder<>() {
            @Override
            public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                st.setObject(index, javaType.unwrap(value, Integer.class, options), getJdbcTypeCode());
            }

            @Override
            public void bind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
                st.setObject(name, javaType.unwrap(value, Integer.class, options), getJdbcTypeCode());
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
