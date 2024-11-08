package tech.ydb.hibernate.dialect.code;

/**
 * @author Kirill Kurdyukov
 */
public final class YdbJdbcCode {

    /**
     * Boolean value.
     */
    public static final int BOOL = 10000;

    /**
     * A signed integer. Acceptable values: from -2^7 to 2^7–1. Not supported for table columns
     */
    public static final int INT8 = 10001;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^8–1.
     */
    public static final int UINT8 = 10002;

    /**
     * A signed integer. Acceptable values: from –2^15 to 2^15–1. Not supported for table columns
     */
    public static final int INT16 = 10003;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^16–1. Not supported for table columns
     */
    public static final int UINT16 = 10004;

    /**
     * A signed integer. Acceptable values: from –2^31 to 2^31–1.
     */
    public static final int INT32 = 10005;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^32–1.
     */
    public static final int UINT32 = 10006;

    /**
     * A signed integer. Acceptable values: from –2^63 to 2^63–1.
     */
    public static final int INT64 = 10007;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^64–1.
     */
    public static final int UINT64 = 10008;

    /**
     * A real number with variable precision, 4 bytes in size. Can't be used in the primary key
     */
    public static final int FLOAT = 10009;

    /**
     * A real number with variable precision, 8 bytes in size. Can't be used in the primary key
     */
    public static final int DOUBLE = 10010;

    /**
     * A binary data, synonym for YDB type String
     */
    public static final int BYTES = 10011;

    /**
     * Text encoded in UTF-8, synonym for YDB type Utf8
     */
    public static final int TEXT = 10012;

    /**
     * YSON in a textual or binary representation. Doesn't support matching, can't be used in the primary key
     */
    public static final int YSON = 10013;

    /**
     * JSON represented as text. Doesn't support matching, can't be used in the primary key
     */
    public static final int JSON = 10014;

    /**
     * Universally unique identifier UUID. Not supported for table columns
     */
    public static final int UUID = 10015;

    /**
     * Date, precision to the day
     */
    public static final int DATE = 10016;

    /**
     * Date/time, precision to the second
     */
    public static final int DATETIME = 10017;

    /**
     * Date/time, precision to the microsecond
     */
    public static final int TIMESTAMP = 10018;

    /**
     * Time interval (signed), precision to microseconds
     */
    public static final int INTERVAL = 10019;

    /**
     * Date with time zone label, precision to the day
     */
    public static final int TZ_DATE = 10020;

    /**
     * Date/time with time zone label, precision to the second
     */
    public static final int TZ_DATETIME = 10021;

    /**
     * Date/time with time zone label, precision to the microsecond
     */
    public static final int TZ_TIMESTAMP = 10022;

    /**
     * JSON in an indexed binary representation. Doesn't support matching, can't be used in the primary key
     */
    public static final int JSON_DOCUMENT = 10023;

    public static final int DECIMAL_SHIFT = (1 << 14);

    /**
     *  <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/v2.3.3/jdbc/src/main/java/tech/ydb/jdbc/impl/YdbTypes.java#L37-L66">link</a>
     */
    public static final int DECIMAL_22_9 = DECIMAL_SHIFT + (22 << 6) + 9;

    public static final int DECIMAL_31_9 = DECIMAL_SHIFT + (31 << 6) + 9;

    public static final int DECIMAL_35_0 = DECIMAL_SHIFT + (35 << 6);

    public static final int DECIMAL_35_9 = DECIMAL_SHIFT + (35 << 6) + 9;
}
