package tech.ydb.hibernate.dialect.hint;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kirill Kurdyukov
 */
public final class QueryHints {
    private static final Pattern SELECT_FROM_WHERE = Pattern
            .compile("^\\s*(select.+?from\\s+\\w+)(.+where.+)$", Pattern.CASE_INSENSITIVE);

    private QueryHints() {
    }

    public static String addViewIndexesToQuery(String query, List<String> indexNames) {
        Matcher matcher = SELECT_FROM_WHERE.matcher(query);
        if (!matcher.matches() || matcher.groupCount() < 2) {
            return query;
        }
        return matcher.group(1) + " view " + String.join(", ", indexNames) + matcher.group(2);
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
