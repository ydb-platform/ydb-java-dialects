package tech.ydb.data.relational.core.dialect;

import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;

/**
 * @author Madiyar Nurgazin
 */
public class YdbDialect extends AbstractDialect {

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
            return String.format("LIMIT %s OFFSET %s", offset, limit);
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    private static final LockClause LOCK_CLAUSE = new LockClause() {
        public String getLock(LockOptions lockOptions) {
            return "";
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

    public IdentifierProcessing getIdentifierProcessing() {
        return IdentifierProcessing.create(
                new IdentifierProcessing.Quoting("`"),
                IdentifierProcessing.LetterCasing.UPPER_CASE
        );
    }


}
