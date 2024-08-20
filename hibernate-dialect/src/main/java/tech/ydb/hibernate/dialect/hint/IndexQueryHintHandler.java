package tech.ydb.hibernate.dialect.hint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kirill Kurdyukov
 */
public class IndexQueryHintHandler {
    private static final Pattern SELECT_FROM_WHERE_QUERY_PATTERN = Pattern
            .compile("^\\s*(select.+?from\\s+\\w+)(.+where.+)$", Pattern.CASE_INSENSITIVE);

    public static final String HINT_USE_INDEX = "use_index:";
    public static final IndexQueryHintHandler INSTANCE = new IndexQueryHintHandler();

    public String addQueryHints(String query, List<String> hints) {
        if (hints.isEmpty()) {
            return query;
        }

        var useIndexes = new ArrayList<String>();
        hints.forEach(hint -> {
            if (hint.startsWith(HINT_USE_INDEX)) {
                useIndexes.add(hint.substring(HINT_USE_INDEX.length()));
            }
        });

        if (!useIndexes.isEmpty()) {
            Matcher matcher = SELECT_FROM_WHERE_QUERY_PATTERN.matcher(query);
            if (matcher.matches() && matcher.groupCount() > 1) {
                String startToken = matcher.group(1);
                String endToken = matcher.group(2);

                return startToken + " view " + String.join(", ", useIndexes) + endToken;
            }
        }

        return query;
    }
}
