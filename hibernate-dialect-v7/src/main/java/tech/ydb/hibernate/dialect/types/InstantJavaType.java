package tech.ydb.hibernate.dialect.types;

import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Ainur Mukhtarov
 */
public class InstantJavaType extends org.hibernate.type.descriptor.java.InstantJavaType {

    public static final InstantJavaType INSTANCE = new InstantJavaType();

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
        return InstantJdbcType.INSTANCE;
    }
}
