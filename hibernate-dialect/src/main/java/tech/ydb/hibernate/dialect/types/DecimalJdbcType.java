package tech.ydb.hibernate.dialect.types;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.MappingException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Kirill Kurdyukov
 */
public class DecimalJdbcType extends org.hibernate.type.descriptor.jdbc.DecimalJdbcType implements AdjustableJdbcType {
    private final int sqlCode;
    private final int defaultSqlCode;

    public DecimalJdbcType(int sqlCode) {
        this.sqlCode = sqlCode;
        this.defaultSqlCode = sqlCode;
    }

    public DecimalJdbcType(int defaultSqlCode, int precision, int scale) {
        this.sqlCode = jdbcTypeCode(precision, scale);
        this.defaultSqlCode = defaultSqlCode;
    }

    @Override
    public int getJdbcTypeCode() {
        return sqlCode;
    }

    @Override
    public int getDefaultSqlTypeCode() {
        return defaultSqlCode;
    }

    @Override
    public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> javaType) {
        if (sqlCode != Types.NUMERIC && sqlCode != Types.DECIMAL) {
            return this;
        }

        int precision = indicators.getColumnPrecision();
        int scale = indicators.getColumnScale();
        if (precision == JdbcTypeIndicators.NO_COLUMN_PRECISION) {
            precision = javaType.getDefaultSqlPrecision(indicators.getDialect(), this);
        }
        if (scale == JdbcTypeIndicators.NO_COLUMN_SCALE) {
            scale = javaType.getDefaultSqlScale(indicators.getDialect(), this);
        }
        return new DecimalJdbcType(defaultSqlCode, precision, scale);
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

    private static int jdbcTypeCode(int precision, int scale) {
        if (precision < 1 || precision > 35 || scale < 0 || scale > precision) {
            throw new MappingException("Invalid YDB Decimal(" + precision + ", " + scale
                    + "): precision must be between 1 and 35 and scale between 0 and precision");
        }
        return (1 << 14) + (precision << 6) + scale;
    }
}
