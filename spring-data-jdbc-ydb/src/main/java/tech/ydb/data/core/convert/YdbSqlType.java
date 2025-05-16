package tech.ydb.data.core.convert;

import java.io.Serializable;
import java.sql.SQLType;

/**
 *
 * @author Aleksandr Gorshenin
 */
class YdbSqlType implements SQLType, Serializable {
    private static final long serialVersionUID = -5722445668088782880L;

    private final String name;
    private final int vendorCode;

    public YdbSqlType(YQLType type) {
        this.name = type.name();
        this.vendorCode = type.getSqlType();
    }

    public YdbSqlType(int decimalPrecision, int decimalScale) {
        this.name = "Decimal(" + decimalPrecision + "," + decimalScale + ")";
        this.vendorCode = YdbConst.ydbDecimal(decimalPrecision, decimalScale);
    }

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
        return vendorCode;
    }
}
