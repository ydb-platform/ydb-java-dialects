package tech.ydb.hibernate.dialect.hint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * @author Kirill Kurdyukov
 */
public final class QueryHints {
    private static final Logger LOGGER = Logger.getLogger(QueryHints.class);

    private QueryHints() {
    }

    public static String addViewIndexesToQuery(String query, List<String> indexHints) {
        Map<String, List<IndexTypedHint>> typedHints = new LinkedHashMap<>();
        String shortIndex = null;

        for (String body : indexHints) {
            int tableColon = body.indexOf(':');
            int columnsStart = body.indexOf('(', tableColon + 1);
            int columnsEnd = body.lastIndexOf(')');

            if (tableColon > 0 && columnsStart > tableColon && columnsEnd > columnsStart) {
                String indexName = body.substring(0, tableColon).trim();
                String tableName = body.substring(tableColon + 1, columnsStart).trim();
                List<String> columns = splitColumns(body.substring(columnsStart + 1, columnsEnd));
                if (!indexName.isEmpty() && !tableName.isEmpty() && !columns.isEmpty()) {
                    List<IndexTypedHint> hintsForTable = typedHints
                            .computeIfAbsent(tableName, k -> new ArrayList<>());
                    hintsForTable.add(new IndexTypedHint(indexName, columns));
                    continue;
                }
            }

            if (shortIndex != null) {
                LOGGER.warnf("Only one short index hint is supported; " +
                        "index '%s' will be used and index '%s' will be ignored", shortIndex, body);

                continue;
            }

            shortIndex = body;
        }

        if (typedHints.isEmpty() && shortIndex == null) {
            return query;
        }

        return IndexHintApplier.apply(query, typedHints, shortIndex);
    }

    private static List<String> splitColumns(String raw) {
        List<String> columns = new ArrayList<>();
        for (String part : raw.split(",")) {
            String column = part.trim();
            if (!column.isEmpty()) {
                columns.add(column);
            }
        }
        return columns;
    }

    public static String addScanToQuery(String query) {
        return "scan " + query;
    }

    public static String addPragmasToQuery(String query, List<String> pragmas) {
        StringBuilder rewritten = new StringBuilder();
        for (String pragma : pragmas) {
            rewritten.append("PRAGMA ").append(pragma).append(";\n");
        }
        rewritten.append(query);
        return rewritten.toString();
    }
}
