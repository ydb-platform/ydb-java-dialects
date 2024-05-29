package tech.ydb.jooq.binding;

import tech.ydb.jdbc.YdbConst;
import tech.ydb.table.values.PrimitiveType;

public final class BindingTools {
    private BindingTools() {
    }

    public static int indexType(PrimitiveType type) {
        return type.ordinal() + YdbConst.SQL_KIND_PRIMITIVE;
    }
}
