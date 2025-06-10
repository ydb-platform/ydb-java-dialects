package tech.ydb.jooq.codegen;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jooq.Binding;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.jooq.meta.AbstractTableDefinition;
import org.jooq.meta.ColumnDefinition;
import org.jooq.meta.DataTypeDefinition;
import org.jooq.meta.DefaultColumnDefinition;
import org.jooq.meta.DefaultDataTypeDefinition;
import org.jooq.meta.SchemaDefinition;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.jooq.types.UShort;

import tech.ydb.jdbc.common.YdbTypes;
import tech.ydb.jooq.binding.Date32Binding;
import tech.ydb.jooq.binding.DateBinding;
import tech.ydb.jooq.binding.Datetime64Binding;
import tech.ydb.jooq.binding.DatetimeBinding;
import tech.ydb.jooq.binding.Interval64Binding;
import tech.ydb.jooq.binding.IntervalBinding;
import tech.ydb.jooq.binding.JsonBinding;
import tech.ydb.jooq.binding.JsonDocumentBinding;
import tech.ydb.jooq.binding.Timestamp64Binding;
import tech.ydb.jooq.binding.TimestampBinding;
import tech.ydb.jooq.binding.Uint16Binding;
import tech.ydb.jooq.binding.Uint32Binding;
import tech.ydb.jooq.binding.Uint64Binding;
import tech.ydb.jooq.binding.Uint8Binding;
import tech.ydb.jooq.binding.UuidBinding;
import tech.ydb.jooq.binding.YsonBinding;
import tech.ydb.jooq.value.YSON;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.Type;

public class YdbTableDefinition extends AbstractTableDefinition {

    private final TableDescription tableDescription;
    private final String tablePath;
    private final YdbTypes ydbTypes;

    public YdbTableDefinition(
            SchemaDefinition schema,
            String name,
            String comment,
            TableDescription tableDescription,
            String tablePath,
            YdbTypes ydbTypes
    ) {
        super(schema, name, comment);
        this.tableDescription = tableDescription;
        this.tablePath = tablePath;
        this.ydbTypes = ydbTypes;
    }

    @Override
    public String getOutputName() {
        return tablePath;
    }

    @Override
    protected List<ColumnDefinition> getElements0() {
        List<ColumnDefinition> result = new ArrayList<>();

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

            int scale = kind == Type.Kind.DECIMAL ? ((DecimalType) type).getScale() : 0;

            String typeName = type.toString();
            Class<? extends Binding<?, ?>> binding = getBinding(typeName);
            Class<?> javaType = getJavaType(typeName);

            DataTypeDefinition typeDefinition = new DefaultDataTypeDefinition(
                    getDatabase(),
                    null,
                    typeName,
                    ydbTypes.getSqlPrecision(type),
                    ydbTypes.getSqlPrecision(type),
                    scale,
                    isNullable,
                    null,
                    DSL.name(typeName),
                    null,
                    binding != null ? binding.getName() : null,
                    javaType != null ? javaType.getName() : null
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

    private static Class<? extends Binding<?, ?>> getBinding(String typeName) {
        return switch (typeName) {
            case "Date" -> DateBinding.class;
            case "Datetime" -> DatetimeBinding.class;
            case "Interval" -> IntervalBinding.class;
            case "Json" -> JsonBinding.class;
            case "JsonDocument" -> JsonDocumentBinding.class;
            case "Timestamp" -> TimestampBinding.class;
            case "Uint8" -> Uint8Binding.class;
            case "Uint16" -> Uint16Binding.class;
            case "Uint32" -> Uint32Binding.class;
            case "Uint64" -> Uint64Binding.class;
            case "Yson" -> YsonBinding.class;
            case "Uuid" -> UuidBinding.class;
            case "Date32" -> Date32Binding.class;
            case "Datetime64" -> Datetime64Binding.class;
            case "Timestamp64" -> Timestamp64Binding.class;
            case "Interval64" -> Interval64Binding.class;
            default -> null;
        };
    }

    private static Class<?> getJavaType(String typeName) {
        return switch (typeName) {
            case "Date", "Date32" -> LocalDate.class;
            case "Datetime", "Datetime64" -> LocalDateTime.class;
            case "Interval", "Interval64" -> Duration.class;
            case "Json" -> JSON.class;
            case "JsonDocument" -> JSONB.class;
            case "Timestamp", "Timestamp64" -> Instant.class;
            case "Uint8" -> UByte.class;
            case "Uint16" -> UShort.class;
            case "Uint32" -> UInteger.class;
            case "Uint64" -> ULong.class;
            case "Yson" -> YSON.class;
            default -> null;
        };
    }
}
