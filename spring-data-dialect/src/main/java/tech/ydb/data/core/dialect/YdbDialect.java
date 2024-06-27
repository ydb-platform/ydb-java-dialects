package tech.ydb.data.core.dialect;

import java.util.Collections;
import java.util.Set;

import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.InsertRenderContext;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.dialect.OrderByNullPrecedence;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;

/**
 * @author Madiyar Nurgazin
 */
public class YdbDialect extends AbstractDialect {
    public static final YdbDialect INSTANCE = new YdbDialect();

    private static final LimitClause LIMIT_CLAUSE = new LimitClause() {

        @Override
        public String getLimit(long limit) {
            return "LIMIT " + limit;
        }

        @Override
        public String getOffset(long offset) {
            return "OFFSET " + offset;
        }

        @Override
        public String getLimitOffset(long limit, long offset) {
            return String.format("LIMIT %s OFFSET %s", limit, offset);
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    private static final LockClause LOCK_CLAUSE = new LockClause() {
        public String getLock(LockOptions lockOptions) {
            throw new UnsupportedOperationException("YDB does not support pessimistic locks");
        }

        public LockClause.Position getClausePosition() {
            return null;
        }
    };

    @Override
    public LimitClause limit() {
        return LIMIT_CLAUSE;
    }

    @Override
    public LockClause lock() {
        return LOCK_CLAUSE;
    }

    @Override
    public IdentifierProcessing getIdentifierProcessing() {
        return IdentifierProcessing.create(
                new IdentifierProcessing.Quoting("`"),
                IdentifierProcessing.LetterCasing.AS_IS
        );
    }

    @Override
    public Set<Class<?>> simpleTypes() {
        return Collections.emptySet();
    }

    @Override
    public InsertRenderContext getInsertRenderContext() {
        return () -> {
            throw new UnsupportedOperationException("YDB does not support VALUES (DEFAULT) statement");
        };
    }

    @Override
    public OrderByNullPrecedence orderByNullHandling() {
        return OrderByNullPrecedence.NONE;
    }
}
