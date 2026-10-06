package tech.ydb.trino;

import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.spi.statistics.ColumnStatistics;
import io.trino.spi.statistics.Estimate;
import io.trino.spi.statistics.TableStatistics;
import io.trino.spi.type.FixedWidthType;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.values.OptionalType;

import java.util.List;
import java.util.Map;

import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.RealType.REAL;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

final class YdbTableStatistics {
    private YdbTableStatistics() {}

    static TableStatistics fromDescription(TableDescription description, List<JdbcColumnHandle> columns) {
        TableDescription.TableStats nativeStats = description.getTableStats();
        // A zero estimate can be a newly populated table whose background statistics are not ready.
        if (nativeStats == null || nativeStats.getRowsEstimate() <= 0) {
            return TableStatistics.empty();
        }
        double rowCount = nativeStats.getRowsEstimate();
        TableStatistics.Builder statistics = TableStatistics.builder().setRowCount(Estimate.of(rowCount));
        Map<String, TableColumn> nativeColumns = description.getColumns().stream()
                .collect(toMap(TableColumn::getName, identity()));
        List<String> primaryKeys = description.getPrimaryKeys();
        for (JdbcColumnHandle column : columns) {
            TableColumn nativeColumn = nativeColumns.get(column.getColumnName());
            if (nativeColumn == null || nativeColumn.getType() instanceof OptionalType) {
                continue;
            }
            ColumnStatistics.Builder columnStatistics = ColumnStatistics.builder().setNullsFraction(Estimate.zero());
            if (primaryKeys.size() == 1 && primaryKeys.getFirst().equals(column.getColumnName())
                    && !column.getColumnType().equals(REAL) && !column.getColumnType().equals(DOUBLE)) {
                // Uniqueness supplies NDV only for a single non-null key, not individual composite-key columns.
                columnStatistics.setDistinctValuesCount(Estimate.of(rowCount));
            }
            if (column.getColumnType() instanceof FixedWidthType type) {
                // Trino costs logical column bytes, not YDB's compressed physical table size.
                columnStatistics.setDataSize(Estimate.of(rowCount * type.getFixedSize()));
            }
            statistics.setColumnStatistics(column, columnStatistics.build());
        }
        return statistics.build();
    }
}
