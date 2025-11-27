package tech.ydb.data.core.dialect;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.data.jdbc.core.convert.JdbcArrayColumns;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
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
 * @author Mikhail Polivakha
 */
public class YdbDialect extends AbstractDialect implements JdbcDialect {
    public static final YdbDialect INSTANCE = new YdbDialect();

    private static final IdentifierProcessing.Quoting QUOTING = new IdentifierProcessing.Quoting("`");

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
        @Override
        public String getLock(LockOptions lockOptions) {
            throw new UnsupportedOperationException("YDB does not support pessimistic locks");
        }

        @Override
        public LockClause.Position getClausePosition() {
            return null;
        }
    };

    @Override
    protected Function<Select, CharSequence> getAfterFromTable() {
        return select -> {
            var tables = select.getFrom().getTables();

            if (tables.size() != 1) {
                return "";
            }

            Method repositoryMethod = null;
            try {
                repositoryMethod = ExposeInvocationInterceptor.currentInvocation().getMethod();
            } catch (IllegalStateException e) {
                // the assumption is that JdbcRepositoryBeanPostProcessor made a choice to not expose metadata for this Spring Data JDBC repository
            }

            if (repositoryMethod == null) {
                return "";
            }

            var viewIndex = repositoryMethod.getAnnotation(ViewIndex.class);

            if (viewIndex != null && (viewIndex.tableName().isEmpty() ||
                    viewIndex.tableName().equals(tables.get(0).getName().toSql(IdentifierProcessing.NONE)))) {
                return " VIEW " + getIdentifierProcessing().quote(viewIndex.indexName()) +
                        " AS " + tables.get(0).getReferenceName().toSql(INSTANCE.getIdentifierProcessing());
            }

            return "";
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
        return new IdentifierProcessing() {
            @Override
            public String quote(String identifier) {
                return QUOTING.apply(identifier);
            }

            @Override
            public String standardizeLetterCase(String identifier) {
                return identifier;
            }
        };
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

    @Override
    @SuppressWarnings("removal")
    public JdbcArrayColumns getArraySupport() {
        return JdbcArrayColumns.Unsupported.INSTANCE;
    }
}
