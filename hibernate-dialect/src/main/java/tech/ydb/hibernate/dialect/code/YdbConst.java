package tech.ydb.hibernate.dialect.code;

import org.hibernate.Internal;

/**
 * That class contain custom YDB type codes
 * @see <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/3d74021/jdbc/src/main/java/tech/ydb/jdbc/YdbConst.java#L8-L9">JDBC Driver constants</a>
 * @see <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/3d74021/jdbc/src/main/java/tech/ydb/jdbc/impl/YdbTypes.java#L37-L66">Primitive types</a>
 *
 * @author Kirill Kurdyukov
 */
@Internal
public final class YdbConst {
    static final int SQL_KIND_PRIMITIVE = 10000;
    private static final int SQL_KIND_DECIMAL = 1 << 14;
    private static final int DECIMAL_SCALE_BITS = 6;

    private YdbConst() { };

    /**
     * Encodes the Decimal flag, precision, and scale expected by the YDB JDBC driver.
     * Scale occupies the lowest six bits, followed by precision.
     *
     * @see <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/6786ca21b86469723c658312b63f7a7393aa4824/jdbc/src/main/java/tech/ydb/jdbc/common/YdbTypes.java#L149-L155">JDBC Decimal decoding</a>
     */
    public static int decimalJdbcTypeCode(int precision, int scale) {
        return SQL_KIND_DECIMAL + (precision << DECIMAL_SCALE_BITS) + scale;
    }
}
