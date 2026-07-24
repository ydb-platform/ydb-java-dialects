package tech.ydb.hibernate.dialect.hint;

import java.util.List;

class IndexTypedHint {
    final String indexName;
    final List<String> columns;

    IndexTypedHint(String indexName, List<String> columns) {
        this.indexName = indexName;
        this.columns = columns;
    }
}
