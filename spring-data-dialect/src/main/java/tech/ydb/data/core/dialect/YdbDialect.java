package tech.ydb.data.core.dialect;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.InsertRenderContext;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.dialect.OrderByNullPrecedence;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;
import org.springframework.data.relational.core.sql.Select;
import tech.ydb.data.repository.ViewIndex;

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
    protected Function<Select, CharSequence> getAfterFromTable() {
        final var invokeMethod = new AtomicBoolean();
        return select -> {
            if (invokeMethod.get()) { // @MappedCollection without VIEW INDEX!
                return "";
            }

            var tables = select.getFrom().getTables();
            if (tables.size() != 1) {
                return "";
            }

            var viewIndex = ExposeInvocationInterceptor.currentInvocation()
                    .getMethod()
                    .getAnnotation(ViewIndex.class);

            invokeMethod.set(true);

            return viewIndex != null ? " VIEW " + viewIndex.value() + " AS " + tables.get(0).getReferenceName()
                    .toSql(INSTANCE.getIdentifierProcessing()) : "";
        };
    }

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
