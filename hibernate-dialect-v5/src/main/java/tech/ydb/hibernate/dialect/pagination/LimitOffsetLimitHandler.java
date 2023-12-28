package tech.ydb.hibernate.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;

/**
 * @author Kirill Kurdyukov
 */
public class LimitOffsetLimitHandler extends AbstractLimitHandler {

    public final static LimitOffsetLimitHandler INSTANCE = new LimitOffsetLimitHandler();

    @Override
    public String processSql(String sql, RowSelection selection) {
        final boolean hasOffset = LimitHelper.hasFirstRow(selection);
        return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
    }

    @Override
    public boolean supportsLimit() {
        return true;
    }

    @Override
    public boolean bindLimitParametersInReverseOrder() {
        return true; // correct LIMIT ? OFFSET ?
    }
}
