package tech.ydb.hibernate.dialect.hint;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kirill Kurdyukov
 */
public class PragmaQueryHintHandler implements QueryHintHandler {
    public static final PragmaQueryHintHandler INSTANCE = new PragmaQueryHintHandler();

    private static final String HINT_PRAGMA = "add_pragma:";

    @Override
    public String addQueryHints(String query, List<String> hints) {
        var yqlHints = hints.stream()
                .filter(hint -> hint.startsWith(HINT_PRAGMA))
                .map(hint -> "PRAGMA " + hint.substring(HINT_PRAGMA.length()) + ";\n")
                .collect(Collectors.joining());

        return yqlHints + query;
    }

    @Override
    public boolean commentIsHint(String comment) {
        return comment.startsWith(HINT_PRAGMA);
    }
}
