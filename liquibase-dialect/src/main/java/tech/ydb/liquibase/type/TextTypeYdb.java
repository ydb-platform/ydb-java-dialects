package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;
import liquibase.statement.DatabaseFunction;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Text",
        aliases = {
                "char", "java.sql.Types.CHAR", "bpchar", "character", // CHAR
                "nchar", "java.sql.Types.NCHAR", "nchar2", // NCHAR
                "text", // TEXT
                "varchar", "java.sql.Types.VARCHAR", "java.lang.String", "varchar2", "character varying", // VARCHAR
                "nvarchar", "java.sql.Types.NVARCHAR", "nvarchar2", "national", // NVARCHAR
                "clob", "longvarchar", "longtext", "java.sql.Types.LONGVARCHAR",
                "java.sql.Types.CLOB", "nclob", "longnvarchar", "ntext",
                "java.sql.Types.LONGNVARCHAR", "java.sql.Types.NCLOB",
                "tinytext", "mediumtext", "long varchar", "long nvarchar" // CLOB
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class TextTypeYdb extends BaseTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.STRING;
    }

    @Override
    public String objectToSql(Object value, Database database) {
        if ((value == null) || "null".equalsIgnoreCase(value.toString())) {
            return "NULL";
        }

        if (value instanceof DatabaseFunction) {
            return value.toString();
        }

        return "'" + database.escapeStringForDatabase(value.toString()) + "'";
    }
}
