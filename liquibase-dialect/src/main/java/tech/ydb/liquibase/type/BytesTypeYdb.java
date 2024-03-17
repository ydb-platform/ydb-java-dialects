package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Bytes",
        aliases = {
                "blob", "longblob", "longvarbinary", "String",
                "java.sql.Types.BLOB", "java.sql.Types.LONGBLOB",
                "java.sql.Types.LONGVARBINARY", "java.sql.Types.VARBINARY",
                "java.sql.Types.BINARY", "varbinary", "binary", "image",
                "tinyblob", "mediumblob", "long binary", "long varbinary"
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class BytesTypeYdb extends BaseTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.BLOB;
    }

    @Override
    public String objectToSql(Object value, Database database) {
        if ((value == null) || "null".equalsIgnoreCase(value.toString())) {
            return "NULL";
        }

        return "'" + value + "'";
    }
}
