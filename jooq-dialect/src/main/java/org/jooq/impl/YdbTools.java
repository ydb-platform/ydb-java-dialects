package org.jooq.impl;

import org.jooq.*;
import tech.ydb.jooq.YDB;

public final class YdbTools {

    private YdbTools() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static <T> Field<T>[] combineTyped(Field<T> field, Field<T>... fields) {
        if (fields == null) {
            return new Field[]{field};
        } else {
            Field<T>[] result = new Field[fields.length + 1];
            result[0] = field;
            System.arraycopy(fields, 0, result, 1, fields.length);

            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public static Field<?>[] combine(Field<?> field, Field<?>... fields) {
        return combineTyped((Field<Object>) field, (Field<Object>[]) fields);
    }

    @SuppressWarnings("unchecked")
    public static <T> Field<T>[] fieldsArray(T[] values) {
        Field<T>[] result = new Field[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = YDB.val(values[i]);
        }

        return result;
    }
}
