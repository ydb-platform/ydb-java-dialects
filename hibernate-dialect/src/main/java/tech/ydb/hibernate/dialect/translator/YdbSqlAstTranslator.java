package tech.ydb.hibernate.dialect.translator;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Kurdyukov
 */
public class YdbSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
    private static final Logger logger = LoggerFactory.getLogger(YdbSqlAstTranslator.class);

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
    public void visitLikePredicate(LikePredicate likePredicate) {
        if (likePredicate.isCaseSensitive()) {
            likePredicate.getMatchExpression().accept(this);
            if (likePredicate.isNegated()) {
                appendSql(" not");
            }
            appendSql(" like ");
            likePredicate.getPattern().accept(this);

            renderEscapeCharacter(likePredicate);
        } else {
            likePredicate.getMatchExpression().accept(this);
            if (likePredicate.isNegated()) {
                appendSql(" not");
            }
            appendSql(" ilike ");
            likePredicate.getPattern().accept(this);

            renderEscapeCharacter(likePredicate);
        }
    }

    private void renderEscapeCharacter(LikePredicate likePredicate) {
        if (likePredicate.getEscapeCharacter() == null) {
            return;
        }

        appendSql(" escape ");

        var escapeCharacter = likePredicate.getEscapeCharacter();

        if (escapeCharacter instanceof QueryLiteral<?> queryLiteral &&
                queryLiteral.getLiteralValue() instanceof Character literalValue) {
            switch (literalValue) {
                case '\\', '_', '%' -> {
                    logger.error("YDB don't support escape character: {}, change to '?'", literalValue);

                    appendSql("'?'");
                }
                default -> likePredicate.getEscapeCharacter().accept(this);
            }
        } else {
            likePredicate.getEscapeCharacter().accept(this);
        }
    }

    @Override
    public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
        super.visitQueryLiteral(queryLiteral);
    }
}
