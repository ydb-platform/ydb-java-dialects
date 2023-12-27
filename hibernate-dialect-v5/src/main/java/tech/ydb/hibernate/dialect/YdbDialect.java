package tech.ydb.hibernate.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDialect extends Dialect {
    @Override
    public LimitHandler getLimitHandler() {
        return super.getLimitHandler();
    }
}
