package tech.ydb.hibernate.dialect.hint;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kirill Kurdyukov
 */
public class ScanQueryHintHandler implements QueryHintHandler {
    public static final ScanQueryHintHandler INSTANCE = new ScanQueryHintHandler();

    private static final String HINT_USE_SCAN = "use_scan";

    private ScanQueryHintHandler() {

    }

    @Override
    public String addQueryHints(String query, List<String> hints) {
        if (hints.isEmpty()) {
            return query;
        }

        var useScan = new ArrayList<String>();
        hints.forEach(hint -> {
            if (hint.startsWith(HINT_USE_SCAN)) {
                useScan.add(hint.substring(HINT_USE_SCAN.length()));
            }
        });

        if (useScan.size() == 1) {
            return "scan " + query;
        }

        return query;
    }

    @Override
    public boolean commentIsHint(String comment) {
        return comment.startsWith(HINT_USE_SCAN);
    }
}
