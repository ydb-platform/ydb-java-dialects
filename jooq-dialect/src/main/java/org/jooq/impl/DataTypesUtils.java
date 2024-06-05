package org.jooq.impl;

import org.jooq.Binding;
import org.jooq.DataType;
import tech.ydb.jooq.YDB;

import static org.jooq.impl.DSL.name;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
public final class DataTypesUtils {

    private DataTypesUtils() {
        throw new UnsupportedOperationException();
    }

    public static <T> DataType<T> newDataType(DataType<T> sqlDataType, String typeName) {
        return new DefaultDataType<>(YDB.DIALECT, sqlDataType, typeName);
    }

    @SuppressWarnings("rawtypes")
    public static <T> DataType<T> newDataType(DataType sqlDataType, String typeName, Binding<?, T> binding) {
        return new DefaultDataType<>(
                YDB.DIALECT,
                sqlDataType,
                sqlDataType.getType(),
                binding,
                name(typeName),
                typeName,
                typeName,
                sqlDataType.precisionDefined() ? sqlDataType.precision() : null,
                sqlDataType.scaleDefined() ? sqlDataType.scale() : null,
                sqlDataType.lengthDefined() ? sqlDataType.length() : null,
                sqlDataType.nullability(),
                sqlDataType.defaultValue()
        );
    }
}
