package tech.ydb.hibernate.dialect.hint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kirill Kurdyukov
 */
public final class QueryHints {
    private QueryHints() {
    }

    public static String addViewIndexesToQuery(String query, List<String> indexHints) {
        List<String> shortIndexes = new ArrayList<>();
        Map<String, List<IndexTypedHint>> typedHints = new LinkedHashMap<>();

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
            shortIndexes.add(body);
        }

        if (typedHints.isEmpty() && shortIndexes.isEmpty()) {
            return query;
        }
        return IndexHintApplier.apply(query, typedHints, shortIndexes);
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
