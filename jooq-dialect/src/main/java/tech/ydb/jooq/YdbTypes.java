package tech.ydb.jooq;

import org.jooq.DataType;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.jooq.types.UShort;
import tech.ydb.jooq.binding.*;
import tech.ydb.jooq.value.YSON;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.jooq.impl.DataTypesUtils.newDataType;

public final class YdbTypes {

    private YdbTypes() {
        throw new UnsupportedOperationException();
    }

    public static void initialize() {

    }

    public static final DataType<Boolean> BOOL = newDataType(SQLDataType.BOOLEAN, "bool");
    public static final DataType<Byte> INT8 = newDataType(SQLDataType.TINYINT, "int8");
    public static final DataType<Short> INT16 = newDataType(SQLDataType.SMALLINT, "int16");
    public static final DataType<Integer> INT32 = newDataType(SQLDataType.INTEGER, "int32");
    public static final DataType<Long> INT64 = newDataType(SQLDataType.BIGINT, "int64");

    public static final DataType<UByte> UINT8 = newDataType(SQLDataType.TINYINTUNSIGNED, "uint8", new Uint8Binding());
    public static final DataType<UShort> UINT16 = newDataType(SQLDataType.SMALLINTUNSIGNED, "uint16", new Uint16Binding());
    public static final DataType<UInteger> UINT32 = newDataType(SQLDataType.INTEGERUNSIGNED, "uint32", new Uint32Binding());
    public static final DataType<ULong> UINT64 = newDataType(SQLDataType.BIGINTUNSIGNED, "uint64", new Uint64Binding());

    public static final DataType<Float> FLOAT = newDataType(SQLDataType.REAL, "float");
    public static final DataType<Double> DOUBLE = newDataType(SQLDataType.DOUBLE, "double");
    public static final DataType<BigDecimal> DECIMAL = newDataType(SQLDataType.DECIMAL(22, 9), "decimal");

    public static DataType<BigDecimal> DECIMAL(int precision, int scale) {
        return newDataType(SQLDataType.DECIMAL(precision, scale), "decimal");
    }

    public static final DataType<byte[]> STRING = newDataType(SQLDataType.VARBINARY, "Bytes");
    public static final DataType<String> UTF8 = newDataType(SQLDataType.VARCHAR, "Text");
    public static final DataType<org.jooq.JSON> JSON = newDataType(SQLDataType.JSON, "json", new JsonBinding());
    public static final DataType<org.jooq.JSONB> JSONDOCUMENT = newDataType(SQLDataType.JSONB, "jsondocument", new JsonDocumentBinding());
    public static final DataType<YSON> YSON = newDataType(SQLDataType.OTHER, "yson", new YsonBinding());

    public static final DataType<java.util.UUID> UUID = newDataType(SQLDataType.UUID, "uuid", new UuidBinding());

    public static final DataType<LocalDate> DATE = newDataType(SQLDataType.LOCALDATE, "date", new DateBinding());
    public static final DataType<LocalDateTime> DATETIME = newDataType(SQLDataType.LOCALDATETIME, "datetime", new DatetimeBinding());
    public static final DataType<Instant> TIMESTAMP = newDataType(SQLDataType.INSTANT, "timestamp", new TimestampBinding());
    public static final DataType<Duration> INTERVAL = newDataType(SQLDataType.INTERVAL, "interval", new IntervalBinding());

    public static final DataType<LocalDate> DATE32 = newDataType(SQLDataType.LOCALDATE, "date32", new Date32Binding());
    public static final DataType<LocalDateTime> DATETIME64 = newDataType(SQLDataType.LOCALDATETIME, "datetime64", new Datetime64Binding());
    public static final DataType<Instant> TIMESTAMP64 = newDataType(SQLDataType.INSTANT, "timestamp64", new Timestamp64Binding());
    public static final DataType<Duration> INTERVAL64 = newDataType(SQLDataType.INTERVAL, "interval64", new Interval64Binding());

    public static final DataType<ZonedDateTime> TZ_DATE = newDataType(SQLDataType.OTHER, "tzdate", new TzDateBinding());
    public static final DataType<ZonedDateTime> TZ_DATETIME = newDataType(SQLDataType.OTHER, "tzdateTime", new TzDatetimeBinding());
    public static final DataType<ZonedDateTime> TZ_TIMESTAMP = newDataType(SQLDataType.OTHER, "tztimestamp", new TzTimestampBinding());
}