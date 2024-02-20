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
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;

/**
 * @author Kirill Kurdyukov
 */
public class LocalDateTimeJdbcType extends TimeJdbcType {

    public static final LocalDateTimeJdbcType INSTANCE = new LocalDateTimeJdbcType();

    @Override
    public String toString() {
        return "LocalDateTimeTypeDescriptor";
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(
                    PreparedStatement st,
                    X value,
                    int index,
                    WrapperOptions options
            ) throws SQLException {
                final LocalDateTime localDateTime = javaType.unwrap(value, LocalDateTime.class, options);

                st.setObject(index, localDateTime);
            }

            @Override
            protected void doBind(
                    CallableStatement st,
                    X value,
                    String name,
                    WrapperOptions options
            ) throws SQLException {
                final LocalDateTime localDateTime = javaType.unwrap(value, LocalDateTime.class, options);

                st.setObject(name, localDateTime);
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new ValueExtractor<>() {
            @Override
            public X extract(
                    ResultSet rs,
                    int paramIndex,
                    WrapperOptions options
            ) throws SQLException {
                return javaType.wrap(rs.getObject(paramIndex), options);
            }

            @Override
            public X extract(
                    CallableStatement statement,
                    int paramIndex,
                    WrapperOptions options
            ) throws SQLException {
                return javaType.wrap(statement.getObject(paramIndex), options);
            }

            @Override
            public X extract(
                    CallableStatement statement,
                    String paramName,
                    WrapperOptions options
            ) throws SQLException {
                return javaType.wrap(statement.getObject(paramName), options);
            }
        };
    }
}
