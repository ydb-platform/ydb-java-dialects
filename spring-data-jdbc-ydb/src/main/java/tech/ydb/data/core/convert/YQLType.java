package tech.ydb.data.core.convert;


public enum YQLType {
    /** Boolean value. */
    Bool(YdbConst.SQL_KIND_PRIMITIVE + 0),
    /** A signed integer. Acceptable values: from -2^7 to 2^7–1. Not supported for table columns */
    Int8(YdbConst.SQL_KIND_PRIMITIVE + 1),
    /** An unsigned integer. Acceptable values: from 0 to 2^8–1. */
    Uint8(YdbConst.SQL_KIND_PRIMITIVE + 2),
    /** A signed integer. Acceptable values: from –2^15 to 2^15–1. Not supported for table columns */
    Int16(YdbConst.SQL_KIND_PRIMITIVE + 3),
    /** An unsigned integer. Acceptable values: from 0 to 2^16–1. Not supported for table columns */
    Uint16(YdbConst.SQL_KIND_PRIMITIVE + 4),
    /** A signed integer. Acceptable values: from –2^31 to 2^31–1. */
    Int32(YdbConst.SQL_KIND_PRIMITIVE + 5),
    /** An unsigned integer. Acceptable values: from 0 to 2^32–1. */
    Uint32(YdbConst.SQL_KIND_PRIMITIVE + 6),
    /** A signed integer. Acceptable values: from –2^63 to 2^63–1. */
    Int64(YdbConst.SQL_KIND_PRIMITIVE + 7),
    /** An unsigned integer. Acceptable values: from 0 to 2^64–1. */
    Uint64(YdbConst.SQL_KIND_PRIMITIVE + 8),
    /** A real number with variable precision, 4 bytes in size. Can't be used in the primary key */
    Float(YdbConst.SQL_KIND_PRIMITIVE + 9),
    /** A real number with variable precision, 8 bytes in size. Can't be used in the primary key */
    Double(YdbConst.SQL_KIND_PRIMITIVE + 10),
    /** A binary data, synonym for YDB type String */
    Bytes(YdbConst.SQL_KIND_PRIMITIVE + 11),
    /** Text encoded in UTF-8, synonym for YDB type Utf8 */
    Text(YdbConst.SQL_KIND_PRIMITIVE + 12),
    /** YSON in a textual or binary representation. Doesn't support matching, can't be used in the primary key */
    Yson(YdbConst.SQL_KIND_PRIMITIVE + 13),
    /** JSON represented as text. Doesn't support matching, can't be used in the primary key */
    Json(YdbConst.SQL_KIND_PRIMITIVE + 14),
    /** Universally unique identifier UUID. Not supported for table columns */
    Uuid(YdbConst.SQL_KIND_PRIMITIVE + 15),
    /** Date, precision to the day */
    Date(YdbConst.SQL_KIND_PRIMITIVE + 16),
    /** Date/time, precision to the second */
    Datetime(YdbConst.SQL_KIND_PRIMITIVE + 17),
    /** Date/time, precision to the microsecond */
    Timestamp(YdbConst.SQL_KIND_PRIMITIVE + 18),
    /** Time interval (signed), precision to microseconds */
    Interval(YdbConst.SQL_KIND_PRIMITIVE + 19),
    /** Date with time zone label, precision to the day */
    TzDate(YdbConst.SQL_KIND_PRIMITIVE + 20),
    /** Date/time with time zone label, precision to the second */
    TzDatetime(YdbConst.SQL_KIND_PRIMITIVE + 21),
    /** Date/time with time zone label, precision to the microsecond */
    TzTimestamp(YdbConst.SQL_KIND_PRIMITIVE + 22),
    /** JSON in an indexed binary representation. Doesn't support matching, can't be used in the primary key */
    JsonDocument(YdbConst.SQL_KIND_PRIMITIVE + 23),

    // DyNumber(YdbConst.SQL_KIND_PRIMITIVE + 24), -- not supported by JDBC Driver

    Date32(YdbConst.SQL_KIND_PRIMITIVE + 25),

    Datetime64(YdbConst.SQL_KIND_PRIMITIVE + 26),

    Timestamp64(YdbConst.SQL_KIND_PRIMITIVE + 27),

    Interval64(YdbConst.SQL_KIND_PRIMITIVE + 28),

    Decimal(YdbConst.SQL_DEFAULT_DECIMAL); // special case

    private final int sqlType;

    private YQLType(int sqlType) {
        this.sqlType = sqlType;
    }

    public int getSqlType() {
        return this.sqlType;
    }
}
