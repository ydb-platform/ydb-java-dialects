package tech.ydb.data.core.convert;

/**
 * That class contain custom YDB type codes
 * @see <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/3d74021/jdbc/src/main/java/tech/ydb/jdbc/YdbConst.java#L8-L9">JDBC Driver constants</a>
 * @see <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/3d74021/jdbc/src/main/java/tech/ydb/jdbc/impl/YdbTypes.java#L37-L66">Primitive types</a>
 * @see <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/3d74021/jdbc/src/main/java/tech/ydb/jdbc/impl/YdbTypes.java#L138-L144">Decimal type</a>
 *
 * @author Aleksandr Gorshenin
 */
final class YdbConst {
    public static final int SQL_KIND_PRIMITIVE = 10000;
    public static final int SQL_DEFAULT_DECIMAL = ydbDecimal(22, 9);
    private static final int SQL_KIND_DECIMAL = 1 << 14; // 16384

    public static int ydbDecimal(int precision, int scale) {
        return SQL_KIND_DECIMAL + (precision << 6) + (scale & 0x111111);
    }

    private YdbConst() { };
}
