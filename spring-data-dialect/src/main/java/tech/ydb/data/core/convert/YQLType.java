package tech.ydb.data.core.convert;

import java.sql.SQLType;

/**
 * @author Madiyar Nurgazin
 */
public record YQLType(String name, int type) implements SQLType {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVendor() {
        return "YDB";
    }

    @Override
    public Integer getVendorTypeNumber() {
        return type;
    }
}
