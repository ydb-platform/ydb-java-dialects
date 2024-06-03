package tech.ydb.jooq.codegen;

import org.jooq.SortOrder;
import org.jooq.meta.AbstractIndexDefinition;
import org.jooq.meta.ColumnDefinition;
import org.jooq.meta.DefaultIndexColumnDefinition;
import org.jooq.meta.IndexColumnDefinition;
import org.jooq.meta.SchemaDefinition;
import org.jooq.meta.TableDefinition;
import tech.ydb.table.description.TableIndex;

import java.util.ArrayList;
import java.util.List;

public class YdbIndexDefinition extends AbstractIndexDefinition {
    private final TableIndex tableIndex;

    public YdbIndexDefinition(SchemaDefinition schema, String name, TableDefinition table, TableIndex tableIndex) {
        super(schema, name, table, false);

        this.tableIndex = tableIndex;
    }

    @Override
    protected List<IndexColumnDefinition> getIndexColumns0() {
        List<IndexColumnDefinition> result = new ArrayList<>();

        List<String> columns = tableIndex.getColumns();

        for (String column : columns) {
            ColumnDefinition columnDefinition = getTable().getColumn(column);

            IndexColumnDefinition definition = new DefaultIndexColumnDefinition(
                    this,
                    columnDefinition,
                    SortOrder.DEFAULT,
                    columnDefinition.getPosition()
            );

            result.add(definition);
        }

        return result;
    }
}
