package tech.ydb.jooq;

import org.jooq.Keyword;

import static org.jooq.impl.DSL.keyword;

public final class YdbKeywords {
    private YdbKeywords() {
        throw new UnsupportedOperationException();
    }

    public static final Keyword K_UPSERT = keyword("upsert");
    public static final Keyword K_REPLACE = keyword("replace");
}
