package tech.ydb.hibernate.dialect.translator;

import java.util.List;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
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

    @Override
    protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
        super.renderTableGroupJoin(tableGroupJoin, tableGroupJoinCollector);
    }

    @Override
    public void visitFromClause(FromClause fromClause) {
        super.visitFromClause(fromClause);
    }
}
