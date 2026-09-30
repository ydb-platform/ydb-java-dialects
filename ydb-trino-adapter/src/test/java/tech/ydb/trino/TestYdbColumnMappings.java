package tech.ydb.trino;

import io.trino.plugin.base.mapping.DefaultIdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.ColumnMapping;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.JdbcMetadataConfig;
import io.trino.plugin.jdbc.JdbcMetadataSessionProperties;
import io.trino.plugin.jdbc.LongReadFunction;
import io.trino.plugin.jdbc.LongWriteFunction;
import io.trino.plugin.jdbc.ObjectReadFunction;
import io.trino.plugin.jdbc.ObjectWriteFunction;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.Range;
import io.trino.spi.predicate.ValueSet;
import io.trino.spi.type.Int128;
import io.trino.testing.TestingConnectorSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.trino.spi.type.DecimalType.createDecimalType;
import static io.trino.spi.type.DateType.DATE;
import static io.trino.spi.type.SmallintType.SMALLINT;
import static io.trino.testing.TestingConnectorSession.SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.ydb.jdbc.YdbConst.SQL_KIND_DECIMAL;
import static tech.ydb.jdbc.YdbConst.SQL_KIND_PRIMITIVE;

public class TestYdbColumnMappings {
    private final YdbClient client = new YdbClient(
            new BaseJdbcConfig(),
            _ -> {
                throw new SQLException("This test must not open a connection");
            },
            new YdbQueryBuilder(RemoteQueryModifier.NONE),
            new DefaultIdentifierMapping(),
            RemoteQueryModifier.NONE);

    @Test
    public void testUnsignedBigintRoundTrip() throws Exception {
        JdbcTypeHandle handle = new JdbcTypeHandle(Types.BIGINT, Optional.of("Uint64"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        ColumnMapping mapping = client.toColumnMapping(SESSION, null, handle).orElseThrow();
        assertThat(mapping.getType()).isEqualTo(createDecimalType(20, 0));
        BigInteger maximum = new BigInteger("18446744073709551615");
        ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (_, method, _) -> {
                    assertThat(method.getName()).isEqualTo("getBigDecimal");
                    return new BigDecimal(maximum);
                });
        Int128 value = (Int128) ((ObjectReadFunction) mapping.getReadFunction()).readObject(resultSet, 1);
        assertThat(value.toBigInteger()).isEqualTo(maximum);

        List<List<Object>> calls = new ArrayList<>();
        PreparedStatement statement = statement(calls);
        ObjectWriteFunction writer = (ObjectWriteFunction) mapping.getWriteFunction();
        writer.set(statement, 1, value);
        writer.setNull(statement, 2);
        assertThat(calls).containsExactly(List.of(1, -1L, SQL_KIND_PRIMITIVE + 8), List.of(2, SQL_KIND_PRIMITIVE + 8));
        assertThatThrownBy(() -> writer.set(statement, 1, Int128.valueOf(-1))).isInstanceOf(SQLDataException.class);
        assertThatThrownBy(() -> writer.set(statement, 1, Int128.valueOf(BigInteger.ONE.shiftLeft(64)))).isInstanceOf(SQLDataException.class);
    }

    @Test
    public void testUnsignedWriterBounds() throws Exception {
        List<List<Object>> calls = new ArrayList<>();
        PreparedStatement statement = statement(calls);
        LongWriteFunction writer = (LongWriteFunction) YdbColumnMappings.unsignedColumnMapping(SMALLINT, 2).getWriteFunction();
        writer.set(statement, 1, 255);
        assertThat(calls).containsExactly(List.of(1, 255L, SQL_KIND_PRIMITIVE + 2));
        assertThatThrownBy(() -> writer.set(statement, 1, -1)).isInstanceOf(SQLDataException.class);
        assertThatThrownBy(() -> writer.set(statement, 1, 256)).isInstanceOf(SQLDataException.class);
    }

    @Test
    public void testDecimalWriterPreservesPrecisionAndScale() throws Exception {
        List<List<Object>> calls = new ArrayList<>();
        PreparedStatement statement = statement(calls);
        LongWriteFunction shortWriter = (LongWriteFunction) YdbColumnMappings.decimalColumnMapping(createDecimalType(10, 3)).getWriteFunction();
        shortWriter.set(statement, 1, 12345);
        ObjectWriteFunction longWriter = (ObjectWriteFunction) YdbColumnMappings.decimalColumnMapping(createDecimalType(35, 10)).getWriteFunction();
        longWriter.set(statement, 2, Int128.valueOf(new BigInteger("123456789012345678901234567890")));
        longWriter.setNull(statement, 3);
        assertThat(calls).containsExactly(
                List.of(1, new BigDecimal("12.345"), SQL_KIND_DECIMAL + (10 << 6) + 3),
                List.of(2, new BigDecimal("12345678901234567890.1234567890"), SQL_KIND_DECIMAL + (35 << 6) + 10),
                List.of(3, SQL_KIND_DECIMAL + (35 << 6) + 10));
    }

    @Test
    public void testUtcTimestampReads() throws Exception {
        for (String type : List.of("Datetime", "Datetime64", "Timestamp", "Timestamp64")) {
            ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (_, method, args) -> {
                        assertThat(method.getName()).isEqualTo("getObject");
                        if (type.startsWith("Datetime")) {
                            assertThat(args[1]).isEqualTo(LocalDateTime.class);
                            return LocalDateTime.parse("2020-02-29T12:34:56");
                        }
                        assertThat(args[1]).isEqualTo(Instant.class);
                        return Instant.parse("2020-02-29T12:34:56Z");
                    });
            JdbcTypeHandle handle = new JdbcTypeHandle(Types.TIMESTAMP, Optional.of(type),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
            LongReadFunction reader = (LongReadFunction) YdbColumnMappings.timestampColumnMapping(handle).getReadFunction();
            assertThat(reader.readLong(resultSet, 1)).isEqualTo(Instant.parse("2020-02-29T12:34:56Z").getEpochSecond() * 1_000_000);
        }
    }

    @Test
    public void testDate32PredicateBounds() {
        var session = TestingConnectorSession.builder().setPropertyMetadata(
                new JdbcMetadataSessionProperties(new JdbcMetadataConfig(), Optional.empty()).getSessionProperties()).build();
        JdbcTypeHandle handle = new JdbcTypeHandle(Types.DATE, Optional.of("Date32"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        var controller = YdbColumnMappings.dateColumnMapping(handle).getPredicatePushdownController();
        long day = LocalDate.of(1995, 9, 16).toEpochDay();
        for (Domain domain : List.of(Domain.singleValue(DATE, day), Domain.onlyNull(DATE),
                Domain.create(ValueSet.ofRanges(Range.greaterThan(DATE, day)), false))) {
            var result = controller.apply(session, domain);
            assertThat(result.getPushedDown()).isEqualTo(domain);
            assertThat(result.getRemainingFilter().isAll()).isTrue();
        }
        for (Domain domain : List.of(Domain.singleValue(DATE, (long) Integer.MIN_VALUE),
                Domain.create(ValueSet.ofRanges(Range.lessThan(DATE, (long) Integer.MAX_VALUE)), true))) {
            var result = controller.apply(session, domain);
            assertThat(result.getPushedDown().isAll()).isTrue();
            assertThat(result.getRemainingFilter()).isEqualTo(domain);
        }
    }

    @Test
    public void testDatetimeRejectsFractionalSeconds() {
        JdbcTypeHandle handle = new JdbcTypeHandle(Types.TIMESTAMP, Optional.of("Datetime64"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        LongWriteFunction writer = (LongWriteFunction) YdbColumnMappings.timestampColumnMapping(handle).getWriteFunction();
        assertThatThrownBy(() -> writer.set(statement(new ArrayList<>()), 1, 1)).isInstanceOf(SQLDataException.class);
    }

    private static PreparedStatement statement(List<List<Object>> calls) {
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (_, method, args) -> {
                    assertThat(method.getName()).isIn("setObject", "setNull");
                    calls.add(Arrays.asList(args));
                    return null;
                });
    }
}
