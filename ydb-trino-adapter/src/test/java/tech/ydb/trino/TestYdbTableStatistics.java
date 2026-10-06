package tech.ydb.trino;

import io.trino.plugin.base.mapping.DefaultIdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcStatisticsConfig;
import io.trino.plugin.jdbc.JdbcTableHandle;
import io.trino.plugin.jdbc.RemoteTableName;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.statistics.Estimate;
import io.trino.spi.statistics.TableStatistics;
import org.junit.jupiter.api.Test;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.values.PrimitiveType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.testing.TestingConnectorSession.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbTableStatistics {
    private static final JdbcColumnHandle ID = new JdbcColumnHandle("id", YdbTypeUtils.toTypeHandle(BIGINT).orElseThrow(), BIGINT);
    private static final JdbcColumnHandle PAYLOAD = new JdbcColumnHandle("payload", YdbTypeUtils.toTypeHandle(BIGINT).orElseThrow(), BIGINT);

    @Test
    public void testNativeRowsAndNonNullUniqueKey() {
        TableDescription description = TableDescription.newBuilder()
                .addNonnullColumn("payload", PrimitiveType.Int64)
                .addNonnullColumn("id", PrimitiveType.Int64)
                .setPrimaryKey("id")
                .setTableStats(new TableDescription.TableStats(null, null, 17, 100))
                .build();
        TableStatistics statistics = YdbTableStatistics.fromDescription(description, List.of(PAYLOAD, ID));
        assertThat(statistics.getRowCount()).isEqualTo(Estimate.of(17));
        var key = statistics.getColumnStatistics().get(ID);
        assertThat(key.getDistinctValuesCount()).isEqualTo(Estimate.of(17));
        assertThat(key.getNullsFraction()).isEqualTo(Estimate.zero());
        assertThat(key.getDataSize()).isEqualTo(Estimate.of(136));
        assertThat(statistics.getColumnStatistics().get(PAYLOAD).getDistinctValuesCount().isUnknown()).isTrue();
    }

    @Test
    public void testNullableKeyHasNoInventedColumnStatistics() {
        TableDescription description = TableDescription.newBuilder()
                .addNullableColumn("id", PrimitiveType.Int64)
                .setPrimaryKey("id")
                .setTableStats(new TableDescription.TableStats(null, null, 17, 100))
                .build();
        TableStatistics statistics = YdbTableStatistics.fromDescription(description, List.of(ID));
        assertThat(statistics.getRowCount()).isEqualTo(Estimate.of(17));
        assertThat(statistics.getColumnStatistics()).isEmpty();
    }

    @Test
    public void testCompositeKeyDoesNotMakeIndividualColumnsUnique() {
        TableDescription description = TableDescription.newBuilder()
                .addNonnullColumn("payload", PrimitiveType.Int64)
                .addNonnullColumn("id", PrimitiveType.Int64)
                .setPrimaryKeys("payload", "id")
                .setTableStats(new TableDescription.TableStats(null, null, 17, 100))
                .build();
        TableStatistics statistics = YdbTableStatistics.fromDescription(description, List.of(PAYLOAD, ID));
        for (JdbcColumnHandle column : List.of(PAYLOAD, ID)) {
            assertThat(statistics.getColumnStatistics().get(column).getDistinctValuesCount().isUnknown()).isTrue();
            assertThat(statistics.getColumnStatistics().get(column).getNullsFraction()).isEqualTo(Estimate.zero());
        }
    }

    @Test
    public void testFloatingPointKeyDoesNotAssumeNormalizedUniqueness() {
        TableDescription description = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Double)
                .setPrimaryKey("id")
                .setTableStats(new TableDescription.TableStats(null, null, 17, 100))
                .build();
        JdbcColumnHandle column = new JdbcColumnHandle("id", YdbTypeUtils.toTypeHandle(DOUBLE).orElseThrow(), DOUBLE);
        assertThat(YdbTableStatistics.fromDescription(description, List.of(column))
                .getColumnStatistics().get(column).getDistinctValuesCount().isUnknown()).isTrue();
    }

    @Test
    public void testMissingUninitializedAndUnsignedOverflowCountsStayUnknown() {
        var description = TableDescription.newBuilder().addNonnullColumn("id", PrimitiveType.Int64).setPrimaryKey("id");
        assertThat(YdbTableStatistics.fromDescription(description.build(), List.of(ID))).isEqualTo(TableStatistics.empty());
        for (long rows : List.of(0L, -1L, Long.MIN_VALUE)) {
            assertThat(YdbTableStatistics.fromDescription(description
                    .setTableStats(new TableDescription.TableStats(null, null, rows, 100)).build(), List.of(ID)))
                    .isEqualTo(TableStatistics.empty());
        }
    }

    @Test
    public void testLargeCountsDoNotOverflowColumnSize() {
        TableDescription description = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int64)
                .setPrimaryKey("id")
                .setTableStats(new TableDescription.TableStats(null, null, Long.MAX_VALUE, 100))
                .build();
        TableStatistics statistics = YdbTableStatistics.fromDescription(description, List.of(ID));
        assertThat(statistics.getRowCount()).isEqualTo(Estimate.of(Long.MAX_VALUE));
        assertThat(statistics.getColumnStatistics().get(ID).getDataSize())
                .isEqualTo(Estimate.of((double) Long.MAX_VALUE * 8));
    }

    @Test
    public void testDisabledStatisticsDoNotOpenAConnection() {
        YdbClient client = new YdbClient(new BaseJdbcConfig(), new JdbcStatisticsConfig().setEnabled(false),
                _ -> {
                    throw new SQLException("Disabled statistics must not open a connection");
                },
                new YdbQueryBuilder(RemoteQueryModifier.NONE), new DefaultIdentifierMapping(), RemoteQueryModifier.NONE);
        JdbcTableHandle handle = new JdbcTableHandle(new SchemaTableName("default", "fixture"),
                new RemoteTableName(Optional.empty(), Optional.empty(), "fixture"), Optional.empty());
        assertThat(client.getTableStatistics(SESSION, handle)).isEqualTo(TableStatistics.empty());
    }
}
