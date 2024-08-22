package tech.ydb.data.core.convert;

import java.sql.SQLType;
import tech.ydb.jdbc.YdbConst;
import tech.ydb.table.values.PrimitiveType;

/**
 * @author Madiyar Nurgazin
 */
public record YQLType(PrimitiveType type) implements SQLType {
    @Override
    public String getName() {
        return type.name();
    }

    @Override
    public String getVendor() {
        return "YDB";
    }

    @Override
    public Integer getVendorTypeNumber() {
        return YdbConst.SQL_KIND_PRIMITIVE + type.ordinal();
    }
}
