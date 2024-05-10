package tech.ydb.jooq;

import org.jooq.DataType;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;
import org.jooq.types.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static tech.ydb.jooq.YDB.DIALECT;

public final class YdbTypes {

    private YdbTypes() {
        throw new UnsupportedOperationException();
    }

    public static final DataType<Boolean> BOOL = new DefaultDataType<>(DIALECT, SQLDataType.BOOLEAN, "bool");
    public static final DataType<Byte> INT8 = new DefaultDataType<>(DIALECT, SQLDataType.TINYINT, "int8");
    public static final DataType<Short> INT16 = new DefaultDataType<>(DIALECT, SQLDataType.SMALLINT, "int16");
    public static final DataType<Integer> INT32 = new DefaultDataType<>(DIALECT, SQLDataType.INTEGER, "int32");
    public static final DataType<Long> INT64 = new DefaultDataType<>(DIALECT, SQLDataType.BIGINT, "int64");

    public static final DataType<UByte> UINT8 = new DefaultDataType<>(DIALECT, SQLDataType.TINYINTUNSIGNED, "uint8");
    public static final DataType<UShort> UINT16 = new DefaultDataType<>(DIALECT, SQLDataType.SMALLINTUNSIGNED, "uint16");
    public static final DataType<UInteger> UINT32 = new DefaultDataType<>(DIALECT, SQLDataType.INTEGERUNSIGNED, "uint32");
    public static final DataType<ULong> UINT64 = new DefaultDataType<>(DIALECT, SQLDataType.BIGINTUNSIGNED, "uint64");

    public static final DataType<Float> FLOAT = new DefaultDataType<>(DIALECT, SQLDataType.REAL, "float");
    public static final DataType<Double> DOUBLE = new DefaultDataType<>(DIALECT, SQLDataType.DOUBLE, "double");

    public static final DataType<String> TEXT = new DefaultDataType<>(DIALECT, SQLDataType.VARCHAR, "text");
    public static final DataType<String> UTF8 = new DefaultDataType<>(DIALECT, SQLDataType.VARCHAR, "utf8");
    public static final DataType<byte[]> BYTES = new DefaultDataType<>(DIALECT, SQLDataType.VARBINARY, "bytes");
    public static final DataType<String> JSON = new DefaultDataType<>(DIALECT, SQLDataType.VARCHAR, "json");
    public static final DataType<byte[]> JSONDOCUMENT = new DefaultDataType<>(DIALECT, SQLDataType.VARBINARY, "jsondocument");
    public static final DataType<byte[]> YSON = new DefaultDataType<>(DIALECT, SQLDataType.VARBINARY, "yson");

    public static final DataType<Date> DATE = new DefaultDataType<>(DIALECT, SQLDataType.DATE, "date");
    public static final DataType<LocalDateTime> DATETIME = new DefaultDataType<>(DIALECT, SQLDataType.LOCALDATETIME, "datetime");
    public static final DataType<Timestamp> TIMESTAMP = new DefaultDataType<>(DIALECT, SQLDataType.TIMESTAMP, "timestamp");
    public static final DataType<YearToSecond> INTERVAL = new DefaultDataType<>(DIALECT, SQLDataType.INTERVAL, "interval");
    public static final DataType<BigDecimal> DECIMAL = new DefaultDataType<>(DIALECT, SQLDataType.DECIMAL, "decimal");
}
