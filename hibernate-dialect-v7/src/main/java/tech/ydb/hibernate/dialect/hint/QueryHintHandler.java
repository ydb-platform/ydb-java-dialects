package tech.ydb.hibernate.dialect.hint;

import java.util.List;

/**
 * @author Ainur Mukhtarov
 */
public interface QueryHintHandler {

    String addQueryHints(String query, List<String> hints);

    boolean commentIsHint(String comment);
}
