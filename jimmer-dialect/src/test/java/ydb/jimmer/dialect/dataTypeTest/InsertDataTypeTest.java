package ydb.jimmer.dialect.dataTypeTest;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.AbstractInsertTest;
import ydb.jimmer.dialect.model.type.ydbBool.YdbBooleanClassDraft;
import ydb.jimmer.dialect.model.type.ydbBool.YdbBooleanDraft;
import ydb.jimmer.dialect.model.type.ydbDate32.YdbDateDraft;
import ydb.jimmer.dialect.model.type.ydbDate32.YdbLocalDateDraft;
import ydb.jimmer.dialect.model.type.ydbDatetime64.YdbLocalDateTimeDraft;
import ydb.jimmer.dialect.model.type.ydbDecimal.YdbBigDecimalDraft;
import ydb.jimmer.dialect.model.type.ydbDouble.YdbDoubleClassDraft;
import ydb.jimmer.dialect.model.type.ydbDouble.YdbDoubleDraft;
import ydb.jimmer.dialect.model.type.ydbEnum.Value;
import ydb.jimmer.dialect.model.type.ydbEnum.YdbEnumDraft;
import ydb.jimmer.dialect.model.type.ydbFloat.YdbFloatClassDraft;
import ydb.jimmer.dialect.model.type.ydbFloat.YdbFloatDraft;
import ydb.jimmer.dialect.model.type.ydbInt16.YdbShortClassDraft;
import ydb.jimmer.dialect.model.type.ydbInt16.YdbShortDraft;
import ydb.jimmer.dialect.model.type.ydbInt32.YdbIntDraft;
import ydb.jimmer.dialect.model.type.ydbInt32.YdbIntegerDraft;
import ydb.jimmer.dialect.model.type.ydbInt32.YdbLocalTimeDraft;
import ydb.jimmer.dialect.model.type.ydbInt32.YdbTimeDraft;
import ydb.jimmer.dialect.model.type.ydbInt64.YdbBigIntegerDraft;
import ydb.jimmer.dialect.model.type.ydbInt64.YdbLongClassDraft;
import ydb.jimmer.dialect.model.type.ydbInt64.YdbLongDraft;
import ydb.jimmer.dialect.model.type.ydbInt8.YdbByteClassDraft;
import ydb.jimmer.dialect.model.type.ydbInt8.YdbByteDraft;
import ydb.jimmer.dialect.model.type.ydbInterval64.YdbDurationDraft;
import ydb.jimmer.dialect.model.type.ydbString.YdbByteArrayDraft;
import ydb.jimmer.dialect.model.type.ydbTimestamp64.YdbInstantDraft;
import ydb.jimmer.dialect.model.type.ydbTimestamp64.YdbTimestampDraft;
import ydb.jimmer.dialect.model.type.ydbTimestamp64.YdbUtilDateDraft;
import ydb.jimmer.dialect.model.type.ydbUtf8.YdbStringDraft;
import ydb.jimmer.dialect.model.type.ydbUuid.YdbUuidDraft;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

public class InsertDataTypeTest extends AbstractInsertTest {
    private void typeTest(String tableName,
                          String typeName,
                          Object input,
                          Object[] variables) {
        createTable(tableName, typeName);

        executeAndExpect(
                getYqlClient().getEntities().saveCommand(input)
                        .setMode(SaveMode.INSERT_ONLY),
                cxt -> {
                    cxt.sql("insert into " + tableName + "(id, value) values(?, ?)");
                    cxt.variables(variables);
                }
        );

        dropTable(tableName);
    }

    /**
     * {@link org.babyfish.jimmer.sql.ast.impl.Variables#handleDateTime(Object, ZoneId) handleDateTime(Object, ZoneId)}
     * this Jimmer method is responsible for changing java types without any user input
     */
    @Test
    public void handleDateTimeJimmerTest() {
        Object[] variables = new Object[]{0, Instant.now()};

        typeTest("ydb_instant", "Timestamp64",
                YdbInstantDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Instant) variables[1]);
                }),
                variables);

        Object[] variables1 = new Object[]{0, LocalDateTime.parse("1970-01-01T00:00:00")};

        typeTest("ydb_local_date_time", "DateTime64",
                YdbLocalDateTimeDraft.$.produce(t -> {
                    t.setId((Integer) variables1[0]);
                    t.setValue((LocalDateTime) variables1[1]);
                }),
                variables1);

        Object[] variables2 = new Object[]{0, LocalDate.parse("1970-01-01")};

        typeTest("ydb_local_date", "Date32",
                YdbLocalDateDraft.$.produce(t -> {
                    t.setId((Integer) variables2[0]);
                    t.setValue((LocalDate) variables2[1]);
                }),
                variables2);

        Object[] variables3 = new Object[]{0, LocalTime.parse("10:15")};

        typeTest("ydb_local_time", "Int32",
                YdbLocalTimeDraft.$.produce(t -> {
                    t.setId((Integer) variables3[0]);
                    t.setValue((LocalTime) variables3[1]);
                }),
                variables3);

        Object[] variables4 = new Object[]{0, new java.util.Date(0)};

        typeTest("ydb_util_date", "Timestamp64",
                YdbUtilDateDraft.$.produce(t -> {
                    t.setId((Integer) variables4[0]);
                    t.setValue((java.util.Date) variables4[1]);
                }),
                variables4);
    }

    @Test
    public void boolTest() {
        Object[] variables = new Object[]{0, true};

        typeTest("ydb_boolean", "Bool",
                YdbBooleanDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Boolean) variables[1]);
                }),
                variables
        );

        typeTest("ydb_boolean_class", "Bool",
                YdbBooleanClassDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Boolean) variables[1]);
                }),
                variables);
    }

    @Test
    public void date32Test() {
        Object[] variables1 = new Object[]{0, new Date(0)};

        typeTest("ydb_date", "Date32",
                YdbDateDraft.$.produce(t -> {
                    t.setId((Integer) variables1[0]);
                    t.setValue((Date) variables1[1]);
                }),
                variables1);
    }

    @Test
    public void decimalTest() {
        Object[] variables = new Object[]{0, new BigDecimal(0)};

        typeTest("ydb_big_decimal", "Decimal(22, 9)",
                YdbBigDecimalDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((BigDecimal) variables[1]);
                }),
                variables);
    }

    @Test
    public void doubleTest() {
        Object[] variables = new Object[]{0, 0.1};

        typeTest("ydb_double", "Double",
                YdbDoubleDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((double) variables[1]);
                }),
                variables);

        typeTest("ydb_double_class", "Double",
                YdbDoubleClassDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Double) variables[1]);
                }),
                variables);
    }

    @Test
    public void enumTest() {
        Object[] variables = new Object[]{0, Value.ONE};

        typeTest("ydb_enum", "Utf8",
                YdbEnumDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Value) variables[1]);
                }),
                new Object[]{0, "ONE"});
    }

    @Test
    public void floatTest() {
        Object[] variables = new Object[]{0, (float) 0.1};

        typeTest("ydb_float", "Float",
                YdbFloatDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((float) variables[1]);
                }),
                variables);

        typeTest("ydb_float_class", "Float",
                YdbFloatClassDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Float) variables[1]);
                }),
                variables);
    }

    @Test
    public void int8Test() {
        Object[] variables = new Object[]{0, (byte) 1};

        typeTest("ydb_byte", "Int8",
                YdbByteDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((byte) variables[1]);
                }),
                variables);

        typeTest("ydb_byte_class", "Int8",
                YdbByteClassDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Byte) variables[1]);
                }),
                variables);
    }

    @Test
    public void int16Test() {
        Object[] variables = new Object[]{0, (short) 1};

        typeTest("ydb_short", "Int16",
                YdbShortDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((short) variables[1]);
                }),
                variables);

        typeTest("ydb_short_class", "Int16",
                YdbShortClassDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Short) variables[1]);
                }),
                variables);
    }

    @Test
    public void int32Test() {
        Object[] variables = new Object[]{0, 1};

        typeTest("ydb_int", "Int32",
                YdbIntDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((int) variables[1]);
                }),
                variables);

        typeTest("ydb_integer", "Int32",
                YdbIntegerDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Integer) variables[1]);
                }),
                variables);

        Object[] variables2 = new Object[]{0, new Time(0)};

        typeTest("ydb_time", "Int32",
                YdbTimeDraft.$.produce(t -> {
                    t.setId((Integer) variables2[0]);
                    t.setValue((Time) variables2[1]);
                }),
                variables2);
    }

    @Test
    public void int64Test() {
        Object[] variables = new Object[]{0, new BigInteger("1234567890")};

        typeTest("ydb_big_integer", "Int64",
                YdbBigIntegerDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((BigInteger) variables[1]);
                }),
                variables);

        Object[] variables1 = new Object[]{0, (long) 1};

        typeTest("ydb_long", "Int64",
                YdbLongDraft.$.produce(t -> {
                    t.setId((Integer) variables1[0]);
                    t.setValue((long) variables1[1]);
                }),
                variables1);

        typeTest("ydb_long_class", "Int64",
                YdbLongClassDraft.$.produce(t -> {
                    t.setId((Integer) variables1[0]);
                    t.setValue((Long) variables1[1]);
                }),
                variables1);
    }

    @Test
    public void interval64Test() {
        Object[] variables = new Object[]{0, Duration.ofHours(1)};

        typeTest("ydb_duration", "Interval64",
                YdbDurationDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Duration) variables[1]);
                }),
                variables);
    }

    @Test
    public void stringTest() {
        Object[] variables = new Object[]{0, new byte[]{1, 2}};

        typeTest("ydb_byte_array", "String",
                YdbByteArrayDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((byte[]) variables[1]);
                }),
                variables);
    }

    @Test
    public void timestamp64Test() {
        Object[] variables = new Object[]{0, new Timestamp(0)};

        typeTest("ydb_timestamp", "Timestamp64",
                YdbTimestampDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Timestamp) variables[1]);
                }),
                variables);
    }

    @Test
    public void utf8Test() {
        Object[] variables = new Object[]{0, "ydb"};

        typeTest("ydb_string", "Utf8",
                YdbStringDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((String) variables[1]);
                }),
                variables);
    }

    @Test
    public void uuidTest() {
        Object[] variables = new Object[]{0, UUID.fromString("9e197d65-1914-4d57-a65f-77a52a06baa7")};

        typeTest("ydb_uuid", "Uuid",
                YdbUuidDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((UUID) variables[1]);
                }),
                variables);
    }
}
