package tech.ydb.hibernate.dialect.types;

import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Ainur Mukhtarov
 */
public class LocalDateTimeJavaType extends org.hibernate.type.descriptor.java.LocalDateTimeJavaType {

    public static final LocalDateTimeJavaType INSTANCE = new LocalDateTimeJavaType();

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
        return LocalDateTimeJdbcType.INSTANCE;
    }
}
