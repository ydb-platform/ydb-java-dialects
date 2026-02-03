package tech.ydb.hibernate.dialect.hint;

import java.util.List;

/**
 * @author Kirill Kurdyukov
 */
public interface QueryHintHandler {

    String addQueryHints(String query, List<String> hints);

    boolean commentIsHint(String comment);
}
