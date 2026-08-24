package tech.ydb.trino;

import io.trino.plugin.jdbc.JdbcMetadataConfig;
import io.trino.plugin.jdbc.JdbcMetadataSessionProperties;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.PredicatePushdownController.DomainPushdownResult;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.predicate.Domain;
import io.trino.testing.TestingConnectorSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static io.trino.spi.type.DateType.DATE;
import static io.trino.spi.type.TimestampType.TIMESTAMP_MICROS;
import static io.trino.spi.type.TimestampType.createTimestampType;
import static org.assertj.core.api.Assertions.assertThat;

class TestYdbColumnMappings
{
    private static final ConnectorSession SESSION = TestingConnectorSession.builder()
            .setPropertyMetadata(new JdbcMetadataSessionProperties(
                    new JdbcMetadataConfig(), Optional.empty()).getSessionProperties())
            .build();

    @Test
    void testDatePredicateIsPushedWithinYdbRange()
    {
        Domain supportedDate = Domain.multipleValues(DATE, List.of(
                LocalDate.of(1970, 1, 1).toEpochDay(),
                LocalDate.of(1995, 9, 16).toEpochDay(),
                LocalDate.of(2105, 12, 31).toEpochDay()));

        DomainPushdownResult result = YdbClient.dateColumnMapping("Date")
                .getPredicatePushdownController()
                .apply(SESSION, supportedDate);

        assertThat(result.getPushedDown()).isEqualTo(supportedDate);
        assertThat(result.getRemainingFilter()).isEqualTo(Domain.all(DATE));
    }

    @Test
    void testDatePredicateRemainsInTrino()
    {
        Domain negativeDate = Domain.singleValue(DATE, LocalDate.of(-1996, 9, 14).toEpochDay());

        DomainPushdownResult result = YdbClient.dateColumnMapping("Date")
                .getPredicatePushdownController()
                .apply(SESSION, negativeDate);

        assertThat(result.getPushedDown()).isEqualTo(Domain.all(DATE));
        assertThat(result.getRemainingFilter()).isEqualTo(negativeDate);
    }

    @Test
    void testDatePredicateAboveYdbRangeRemainsInTrino()
    {
        Domain unsupportedDate = Domain.singleValue(DATE, LocalDate.of(2106, 1, 1).toEpochDay());

        DomainPushdownResult result = YdbClient.dateColumnMapping("Date")
                .getPredicatePushdownController()
                .apply(SESSION, unsupportedDate);

        assertThat(result.getPushedDown()).isEqualTo(Domain.all(DATE));
        assertThat(result.getRemainingFilter()).isEqualTo(unsupportedDate);
    }

    @Test
    void testDate32PredicateIsPushedBeforeUnixEpoch()
    {
        Domain signedDate = Domain.multipleValues(DATE, List.of(
                LocalDate.of(1, 1, 1).toEpochDay(),
                LocalDate.of(1969, 12, 31).toEpochDay()));

        DomainPushdownResult result = YdbClient.dateColumnMapping("Date32")
                .getPredicatePushdownController()
                .apply(SESSION, signedDate);

        assertThat(result.getPushedDown()).isEqualTo(signedDate);
        assertThat(result.getRemainingFilter()).isEqualTo(Domain.all(DATE));
    }

    @Test
    void testDate32PredicateOutsideYdbRangeRemainsInTrino()
    {
        Domain unsupportedDate = Domain.singleValue(DATE, LocalDate.of(-144169, 12, 31).toEpochDay());

        DomainPushdownResult result = YdbClient.dateColumnMapping("Date32")
                .getPredicatePushdownController()
                .apply(SESSION, unsupportedDate);

        assertThat(result.getPushedDown()).isEqualTo(Domain.all(DATE));
        assertThat(result.getRemainingFilter()).isEqualTo(unsupportedDate);
    }

    @Test
    void testTemporalMappingsRespectPhysicalPrecision()
    {
        assertThat(YdbClient.timestampColumnMapping("Datetime").getType()).isEqualTo(createTimestampType(0));
        assertThat(YdbClient.timestampColumnMapping("Datetime64").getType()).isEqualTo(createTimestampType(0));
        assertThat(YdbClient.timestampColumnMapping("Timestamp").getType()).isEqualTo(TIMESTAMP_MICROS);
        assertThat(YdbClient.timestampColumnMapping("Timestamp64").getType()).isEqualTo(TIMESTAMP_MICROS);
    }

    @Test
    void testTimestamp64PredicateRespectsPinnedDriverUpperBound()
    {
        long lastDriverSupportedEpochMicro = 4_611_669_811_199_999_998L;
        Domain supportedTimestamp = Domain.singleValue(TIMESTAMP_MICROS, lastDriverSupportedEpochMicro);
        Domain rejectedByDriver = Domain.singleValue(TIMESTAMP_MICROS, lastDriverSupportedEpochMicro + 1);

        DomainPushdownResult supportedResult = YdbClient.timestampColumnMapping("Timestamp64")
                .getPredicatePushdownController()
                .apply(SESSION, supportedTimestamp);
        DomainPushdownResult rejectedResult = YdbClient.timestampColumnMapping("Timestamp64")
                .getPredicatePushdownController()
                .apply(SESSION, rejectedByDriver);

        assertThat(supportedResult.getPushedDown()).isEqualTo(supportedTimestamp);
        assertThat(supportedResult.getRemainingFilter()).isEqualTo(Domain.all(TIMESTAMP_MICROS));
        assertThat(rejectedResult.getPushedDown()).isEqualTo(Domain.all(TIMESTAMP_MICROS));
        assertThat(rejectedResult.getRemainingFilter()).isEqualTo(rejectedByDriver);
    }

    @Test
    void testDefaultTemporalTypeHandlesUseWideYdbTypes()
    {
        JdbcTypeHandle date = YdbTypeUtils.toTypeHandle(DATE).orElseThrow();
        JdbcTypeHandle timestamp = YdbTypeUtils.toTypeHandle(TIMESTAMP_MICROS).orElseThrow();

        assertThat(date.jdbcTypeName()).contains("Date32");
        assertThat(timestamp.jdbcTypeName()).contains("Timestamp64");
    }
}
