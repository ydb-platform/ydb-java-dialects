package tech.ydb.hibernate.dialect.exporter;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.tool.schema.spi.Exporter;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Kirill Kurdyukov
 */
public class YdbIndexExporter implements Exporter<Index> {

    public static final YdbIndexExporter INSTANCE = new YdbIndexExporter();

    @Override
    public String[] getSqlCreateStrings(Index exportable, Metadata metadata) {
        StringBuilder yqlIndexQuery = new StringBuilder();

        String tableName = exportable.getTable().getQuotedName();
        Dialect dialect = metadata.getDatabase().getDialect();

        yqlIndexQuery.append("alter table ")
                .append(tableName)
                .append(" add index ")
                .append(exportable.getQuotedName(dialect))
                .append(" global on (");

        final Map<Column, String> columnOrderMap = exportable.getColumnOrderMap();
        boolean first = true;

        for (Iterator<Column> it = exportable.getColumnIterator(); it.hasNext(); ) {
            Column column = it.next();
            if (first) {
                first = false;
            } else {
                yqlIndexQuery.append(", ");
            }

            yqlIndexQuery.append((column.getQuotedName(dialect)));
            if (columnOrderMap.containsKey(column)) {
                yqlIndexQuery.append(" ").append(columnOrderMap.get(column));
            }
        }

        yqlIndexQuery.append(")");

        return new String[]{yqlIndexQuery.toString()};
    }

    @Override
    public String[] getSqlDropStrings(Index exportable, Metadata metadata) {
        StringBuilder yqlIndexQuery = new StringBuilder();

        String tableName = exportable.getTable().getQuotedName();
        Dialect dialect = metadata.getDatabase().getDialect();

        yqlIndexQuery.append("alter table ")
                .append(tableName)
                .append(" drop index ")
                .append(exportable.getQuotedName(dialect));

        return new String[]{yqlIndexQuery.toString()};
    }
}
