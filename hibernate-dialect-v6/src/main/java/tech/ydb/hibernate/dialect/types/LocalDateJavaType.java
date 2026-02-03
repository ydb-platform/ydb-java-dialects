package tech.ydb.hibernate.dialect.types;

import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Kirill Kurdyukov
 */
public class LocalDateJavaType extends org.hibernate.type.descriptor.java.LocalDateJavaType {

    public static final LocalDateJavaType INSTANCE = new LocalDateJavaType();

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
        return LocalDateJdbcType.INSTANCE;
    }
}
