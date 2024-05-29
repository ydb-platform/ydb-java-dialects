package tech.ydb.jooq.codegen;

import org.jooq.meta.AbstractTableDefinition;
import org.jooq.meta.ColumnDefinition;
import org.jooq.meta.DataTypeDefinition;
import org.jooq.meta.DefaultColumnDefinition;
import org.jooq.meta.DefaultDataTypeDefinition;
import org.jooq.meta.SchemaDefinition;
import tech.ydb.jdbc.YdbConnection;
import tech.ydb.jdbc.YdbConst;
import tech.ydb.jdbc.YdbTypes;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.values.Type;

import java.util.ArrayList;
import java.util.List;

public class YdbTableDefinition extends AbstractTableDefinition {

    private final TableDescription tableDescription;
    private final String tablePath;

    public YdbTableDefinition(SchemaDefinition schema, String name, String comment, TableDescription tableDescription, String tablePath) {
        super(schema, name, comment);
        this.tableDescription = tableDescription;
        this.tablePath = tablePath;
    }

    @Override
    public String getOutputName() {
        return tablePath;
    }

    @Override
    protected List<ColumnDefinition> getElements0() {
        List<ColumnDefinition> result = new ArrayList<>();

        YdbConnection connection = (YdbConnection) getConnection();
        YdbTypes types = connection.getYdbTypes();

        List<TableColumn> columns = tableDescription.getColumns();

        short index = 0;
        for (TableColumn column : columns) {
            Type type = column.getType();
            Type.Kind kind = type.getKind();

            boolean isNullable = kind == Type.Kind.OPTIONAL;
            if (isNullable) {
                type = type.unwrapOptional();
                kind = type.getKind();
            }

            int decimalDigits = kind == Type.Kind.DECIMAL ? YdbConst.SQL_DECIMAL_DEFAULT_PRECISION : 0;

            DataTypeDefinition typeDefinition = new DefaultDataTypeDefinition(
                    getDatabase(),
                    null,
                    type.toString(),
                    types.getSqlPrecision(type),
                    types.getSqlPrecision(type),
                    decimalDigits,
                    isNullable,
                    null
            );

            ColumnDefinition columnDefinition = new DefaultColumnDefinition(
                    getDatabase().getTable(getSchema(), getName()),
                    column.getName(),
                    index,
                    typeDefinition,
                    false,
                    null
            );

            result.add(columnDefinition);
        }

        return result;
    }

    public TableDescription getTableDescription() {
        return tableDescription;
    }
}
