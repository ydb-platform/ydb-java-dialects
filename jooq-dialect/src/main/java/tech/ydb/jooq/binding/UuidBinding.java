package tech.ydb.jooq.binding;

import java.sql.SQLException;
import java.util.UUID;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;

/**
 * @author Kirill Kurdyukov
 */
public class UuidBinding extends AbstractBinding<UUID, UUID> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Uuid);

    @Override
    public Converter<UUID, UUID> converter() {
        return new IdentityConverter<>(UUID.class);
    }

    @Override
    public void set(BindingSetStatementContext<UUID> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), ctx.value(), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<UUID> ctx) throws SQLException {
        ctx.value((UUID) ctx.resultSet().getObject(ctx.index()));
    }
}
