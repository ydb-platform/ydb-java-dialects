package tech.ydb.jooq;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.jooq.DataType;
import org.jooq.Meta;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;
import org.jooq.meta.AbstractMetaDatabase;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.jooq.types.UShort;
import org.jooq.types.YearToSecond;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YDBDatabase extends AbstractMetaDatabase {

    private static final SQLDialect FAMILY = null;

    public static final DataType<Boolean> BOOL = new DefaultDataType<>(FAMILY, SQLDataType.BOOLEAN,   "bool");

    public static final DataType<Byte>     INT8 = new DefaultDataType<>(FAMILY, SQLDataType.TINYINT,  "int8");
    public static final DataType<Short>   INT16 = new DefaultDataType<>(FAMILY, SQLDataType.SMALLINT, "int16");
    public static final DataType<Integer> INT32 = new DefaultDataType<>(FAMILY, SQLDataType.INTEGER,  "int32");
    public static final DataType<Long>    INT64 = new DefaultDataType<>(FAMILY, SQLDataType.BIGINT,   "int64");

    // TODO: doesnt' work without CAST
    public static final DataType<UByte>     UINT8 = new DefaultDataType<>(FAMILY, SQLDataType.TINYINTUNSIGNED,  "uint8");
    // TODO: doesnt' work without CAST
    public static final DataType<UShort>   UINT16 = new DefaultDataType<>(FAMILY, SQLDataType.SMALLINTUNSIGNED, "uint16");
    // TODO: doesnt' work without CAST
    public static final DataType<UInteger> UINT32 = new DefaultDataType<>(FAMILY, SQLDataType.INTEGERUNSIGNED,  "uint32");
    // TODO: doesnt' work without CAST
    public static final DataType<ULong>    UINT64 = new DefaultDataType<>(FAMILY, SQLDataType.BIGINTUNSIGNED,   "uint64");

    public static final DataType<Float>   FLOAT = new DefaultDataType<>(FAMILY, SQLDataType.REAL,  "float");
    public static final DataType<Double> DOUBLE = new DefaultDataType<>(FAMILY, SQLDataType.DOUBLE, "double");

    public static final DataType<String>  TEXT = new DefaultDataType<>(FAMILY, SQLDataType.VARCHAR,   "text");
    public static final DataType<byte[]> BYTES = new DefaultDataType<>(FAMILY, SQLDataType.VARBINARY, "bytes");

    // TODO: doesnt' work without CAST
    public static final DataType<String>  JSON    = new DefaultDataType<>(FAMILY, SQLDataType.VARCHAR, "json");
    // TODO: doesnt' work without CAST
    public static final DataType<String>  JSONDOC = new DefaultDataType<>(FAMILY, SQLDataType.VARCHAR, "jsondocument");
    // TODO: doesnt' work without CAST
    public static final DataType<byte[]>  YSON    = new DefaultDataType<>(FAMILY, SQLDataType.VARBINARY, "yson");

    public static final DataType<Date>          DATE      = new DefaultDataType<>(FAMILY, SQLDataType.DATE, "date");
    public static final DataType<LocalDateTime> DATETIME  = new DefaultDataType<>(FAMILY, SQLDataType.LOCALDATETIME, "datetime");
    public static final DataType<Timestamp>     TIMESTAMP = new DefaultDataType<>(FAMILY, SQLDataType.TIMESTAMP, "timestamp");
    public static final DataType<YearToSecond>  INTERVAL  = new DefaultDataType<>(FAMILY, SQLDataType.INTERVAL, "interval");

    public static final DataType<BigDecimal> DECIMAL = new DefaultDataType<>(FAMILY, SQLDataType.DECIMAL, "decimal");

    @Override
    protected Meta getMeta0() {
        return create0().meta();
    }
}
