package tech.ydb.hibernate.dialect.types;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Kirill Kurdyukov
 */
public class DecimalJdbcType extends org.hibernate.type.descriptor.jdbc.DecimalJdbcType {
    private final int sqlCode;

    public DecimalJdbcType(int sqlCode) {
        this.sqlCode = sqlCode;
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        return new ValueBinder<>() {
            @Override
            public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                st.setObject(index, javaType.unwrap(value, BigDecimal.class, options), sqlCode);
            }

            @Override
            public void bind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
                st.setObject(name, javaType.unwrap(value, BigDecimal.class, options), sqlCode);
            }
        };
    }
}
