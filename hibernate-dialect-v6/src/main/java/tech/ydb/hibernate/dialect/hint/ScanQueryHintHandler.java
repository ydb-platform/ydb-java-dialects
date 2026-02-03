package tech.ydb.hibernate.dialect.hint;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
        AtomicBoolean useScan = new AtomicBoolean(false);
        hints.forEach(hint -> {
            if (hint.equals(HINT_USE_SCAN)) {
                useScan.set(true);
            }
        });

        if (useScan.get()) {
            return "scan " + query;
        }

        return query;
    }

    @Override
    public boolean commentIsHint(String comment) {
        return comment.startsWith(HINT_USE_SCAN);
    }
}
