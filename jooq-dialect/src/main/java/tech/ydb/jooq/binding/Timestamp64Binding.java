package tech.ydb.jooq.binding;

import java.sql.SQLException;
import java.time.Instant;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

/**
 * @author Kirill Kurdyukov
 */
public class Timestamp64Binding extends AbstractBinding<Instant, Instant> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Timestamp64);

    @Override
    public Converter<Instant, Instant> converter() {
        return new IdentityConverter<>(Instant.class);
    }

    @Override
    public void set(BindingSetStatementContext<Instant> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newTimestamp64(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<Instant> ctx) throws SQLException {
        Instant value = (Instant) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }
}
