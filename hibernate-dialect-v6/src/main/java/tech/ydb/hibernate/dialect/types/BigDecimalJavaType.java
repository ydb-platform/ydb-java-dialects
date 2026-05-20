package tech.ydb.hibernate.dialect.types;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Kirill Kurdyukov
 */
public final class BigDecimalJavaType extends org.hibernate.type.descriptor.java.BigDecimalJavaType {

    public static final BigDecimalJavaType INSTANCE_22_9 = new BigDecimalJavaType();

    @Override
    public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
        return 9;
    }

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
        return 22;
    }
}
