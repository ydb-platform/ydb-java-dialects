package tech.ydb.hibernate.dialect.exporter;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Index;
import org.hibernate.tool.schema.spi.Exporter;

import java.util.stream.Collectors;

/**
 * @author Kirill Kurdyukov
 */
public class YdbIndexExporter implements Exporter<Index> {

    public static final YdbIndexExporter INSTANCE = new YdbIndexExporter();

    @Override
    public String[] getSqlCreateStrings(Index exportable, Metadata metadata, SqlStringGenerationContext context) {
        StringBuilder yqlIndexQuery = new StringBuilder();

        String tableName = context.format(exportable.getTable().getQualifiedTableName());
        Dialect dialect = metadata.getDatabase().getDialect();

        yqlIndexQuery.append("alter table ")
                .append(tableName)
                .append(" add index ")
                .append(exportable.getQuotedName(dialect))
                .append(" global on (");

        String columns = exportable.getColumns()
                .stream()
                .map(column -> column.getQuotedName(dialect))
                .collect(Collectors.joining(", "));

        yqlIndexQuery.append(columns)
                .append(")");

        return new String[]{yqlIndexQuery.toString()};
    }

    @Override
    public String[] getSqlDropStrings(Index exportable, Metadata metadata, SqlStringGenerationContext context) {
        StringBuilder yqlIndexQuery = new StringBuilder();

        String tableName = context.format(exportable.getTable().getQualifiedTableName());
        Dialect dialect = metadata.getDatabase().getDialect();

        yqlIndexQuery.append("alter table ")
                .append(tableName)
                .append(" drop index ")
                .append(exportable.getQuotedName(dialect));

        return new String[]{yqlIndexQuery.toString()};
    }
}
