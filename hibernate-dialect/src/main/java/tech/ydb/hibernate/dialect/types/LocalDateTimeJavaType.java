package tech.ydb.hibernate.dialect.types;

import jakarta.persistence.TemporalType;
import java.sql.Types;
import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Kirill Kurdyukov
 */
public class LocalDateTimeJavaType extends org.hibernate.type.descriptor.java.LocalDateTimeJavaType {

    public static final LocalDateTimeJavaType INSTANCE = new LocalDateTimeJavaType();

    @Override
    public TemporalType getPrecision() {
        return TemporalType.TIME;
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
        return context.getJdbcType(Types.TIME);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
        return (TemporalJavaType<X>) this;
    }

    protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
        throw new UnsupportedOperationException(
                this + " as `jakarta.persistence.TemporalType.TIMESTAMP` not supported"
        );
    }
}
