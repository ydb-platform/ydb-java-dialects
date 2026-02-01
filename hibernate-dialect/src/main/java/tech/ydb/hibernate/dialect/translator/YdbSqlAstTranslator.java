package tech.ydb.hibernate.dialect.translator;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * @author Kirill Kurdyukov
 */
public class YdbSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

    protected YdbSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        super(sessionFactory, statement);
    }

    @Override
    protected void renderOffsetFetchClause(
            Expression offsetExpression,
            Expression fetchExpression,
            FetchClauseType fetchClauseType,
            boolean renderOffsetRowsKeyword
    ) {
        // Workaround on this issue:
        // https://github.com/ydb-platform/ydb/issues/33136
        if (offsetExpression != null && fetchExpression == null) {
            appendSql(" limit ");
            appendSql(Long.toString(Long.MAX_VALUE));
        }

        if (fetchExpression != null) {
            appendSql(" limit ");
            getClauseStack().push(Clause.FETCH);
            try {
                fetchExpression.accept(this);
            } finally {
                getClauseStack().pop();
            }
        }

        if (offsetExpression != null) {
            appendSql(" offset ");
            getClauseStack().push(Clause.OFFSET);
            try {
                offsetExpression.accept(this);
            } finally {
                getClauseStack().pop();
            }
        }
    }
}
