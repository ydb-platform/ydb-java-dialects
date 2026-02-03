package tech.ydb.hibernate.dialect.code;

/**
 * @author Kirill Kurdyukov
 */
public final class YdbJdbcCode {

    /**
     * Boolean value.
     */
    public static final int BOOL = YdbConst.SQL_KIND_PRIMITIVE;

    /**
     * A signed integer. Acceptable values: from -2^7 to 2^7–1. Not supported for table columns
     */
    public static final int INT8 = YdbConst.SQL_KIND_PRIMITIVE + 1;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^8–1.
     */
    public static final int UINT8 = YdbConst.SQL_KIND_PRIMITIVE + 2;

    /**
     * A signed integer. Acceptable values: from –2^15 to 2^15–1. Not supported for table columns
     */
    public static final int INT16 = YdbConst.SQL_KIND_PRIMITIVE + 3;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^16–1. Not supported for table columns
     */
    public static final int UINT16 = YdbConst.SQL_KIND_PRIMITIVE + 4;

    /**
     * A signed integer. Acceptable values: from –2^31 to 2^31–1.
     */
    public static final int INT32 = YdbConst.SQL_KIND_PRIMITIVE + 5;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^32–1.
     */
    public static final int UINT32 = YdbConst.SQL_KIND_PRIMITIVE + 6;

    /**
     * A signed integer. Acceptable values: from –2^63 to 2^63–1.
     */
    public static final int INT64 = YdbConst.SQL_KIND_PRIMITIVE + 7;

    /**
     * An unsigned integer. Acceptable values: from 0 to 2^64–1.
     */
    public static final int UINT64 = YdbConst.SQL_KIND_PRIMITIVE + 8;

    /**
     * A real number with variable precision, 4 bytes in size. Can't be used in the primary key
     */
    public static final int FLOAT = YdbConst.SQL_KIND_PRIMITIVE + 9;

    /**
     * A real number with variable precision, 8 bytes in size. Can't be used in the primary key
     */
    public static final int DOUBLE = YdbConst.SQL_KIND_PRIMITIVE + 10;

    /**
     * A binary data, synonym for YDB type String
     */
    public static final int BYTES = YdbConst.SQL_KIND_PRIMITIVE + 11;

    /**
     * Text encoded in UTF-8, synonym for YDB type Utf8
     */
    public static final int TEXT = YdbConst.SQL_KIND_PRIMITIVE + 12;

    /**
     * YSON in a textual or binary representation. Doesn't support matching, can't be used in the primary key
     */
    public static final int YSON = YdbConst.SQL_KIND_PRIMITIVE + 13;

    /**
     * JSON represented as text. Doesn't support matching, can't be used in the primary key
     */
    public static final int JSON = YdbConst.SQL_KIND_PRIMITIVE + 14;

    /**
     * Universally unique identifier UUID. Not supported for table columns
     */
    public static final int UUID = YdbConst.SQL_KIND_PRIMITIVE + 15;

    /**
     * Date, precision to the day
     */
    public static final int DATE = YdbConst.SQL_KIND_PRIMITIVE + 16;

    /**
     * Date/time, precision to the second
     */
    public static final int DATETIME = YdbConst.SQL_KIND_PRIMITIVE + 17;

    /**
     * Date/time, precision to the microsecond
     */
    public static final int TIMESTAMP = YdbConst.SQL_KIND_PRIMITIVE + 18;

    /**
     * Time interval (signed), precision to microseconds
     */
    public static final int INTERVAL = YdbConst.SQL_KIND_PRIMITIVE + 19;

    /**
     * Date with time zone label, precision to the day
     */
    public static final int TZ_DATE = YdbConst.SQL_KIND_PRIMITIVE + 20;

    /**
     * Date/time with time zone label, precision to the second
     */
    public static final int TZ_DATETIME = YdbConst.SQL_KIND_PRIMITIVE + 21;

    /**
     * Date/time with time zone label, precision to the microsecond
     */
    public static final int TZ_TIMESTAMP = YdbConst.SQL_KIND_PRIMITIVE + 22;

    /**
     * JSON in an indexed binary representation. Doesn't support matching, can't be used in the primary key
     */
    public static final int JSON_DOCUMENT = YdbConst.SQL_KIND_PRIMITIVE + 23;

    public static final int DATE_32 = YdbConst.SQL_KIND_PRIMITIVE + 25;

    public static final int DATETIME_64 = YdbConst.SQL_KIND_PRIMITIVE + 26;

    public static final int TIMESTAMP_64 = YdbConst.SQL_KIND_PRIMITIVE + 27;

    public static final int INTERVAL_64 = YdbConst.SQL_KIND_PRIMITIVE + 28;


    // WARN! Attribute value must be constant!

    /**
     * <a href="https://github.com/ydb-platform/ydb-jdbc-driver/blob/v2.3.3/jdbc/src/main/java/tech/ydb/jdbc/impl/YdbTypes.java#L37-L66">link</a>
     */
    public static final int DECIMAL_22_9 = YdbConst.SQL_KIND_DECIMAL + (22 << 6) + 9;

    public static final int DECIMAL_31_9 = YdbConst.SQL_KIND_DECIMAL + (31 << 6) + 9;

    public static final int DECIMAL_35_0 = YdbConst.SQL_KIND_DECIMAL + (35 << 6);

    public static final int DECIMAL_35_9 = YdbConst.SQL_KIND_DECIMAL + (35 << 6) + 9;
}
