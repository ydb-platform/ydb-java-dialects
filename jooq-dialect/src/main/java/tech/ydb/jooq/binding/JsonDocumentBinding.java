package tech.ydb.jooq.binding;

import org.jetbrains.annotations.NotNull;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.JSONB;
import org.jooq.impl.AbstractBinding;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

public class JsonDocumentBinding extends AbstractBinding<byte[], JSONB> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.JsonDocument);

    @NotNull
    @Override
    public Converter<byte[], JSONB> converter() {
        return new JsonDocumentConverter();
    }

    @Override
    public void set(BindingSetStatementContext<JSONB> ctx) throws SQLException {
        ctx.statement().setObject(ctx.index(), PrimitiveValue.newJsonDocument(ctx.value().data()), INDEX_TYPE);
    }

    @Override
    public void get(BindingGetResultSetContext<JSONB> ctx) throws SQLException {
        byte[] value = ctx.resultSet().getBytes(ctx.index());
        ctx.value(JSONB.jsonbOrNull(value == null ? null : new String(value)));
    }

    private static class JsonDocumentConverter implements Converter<byte[], JSONB> {
        @Override
        public JSONB from(byte[] databaseObject) {
            return JSONB.valueOf(new String(databaseObject));
        }

        @Override
        public byte[] to(JSONB userObject) {
            return userObject.data().getBytes();
        }

        @Override
        public Class<byte[]> fromType() {
            return byte[].class;
        }

        @Override
        public Class<JSONB> toType() {
            return JSONB.class;
        }
    }
}

