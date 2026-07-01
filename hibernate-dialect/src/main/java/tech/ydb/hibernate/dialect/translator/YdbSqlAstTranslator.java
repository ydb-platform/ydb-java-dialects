package tech.ydb.hibernate.dialect.translator;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * @author Kirill Kurdyukov
 */
public class YdbSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

    protected YdbSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        super(sessionFactory, statement);
    }

    @Override
    public void visitOffsetFetchClause(QueryPart queryPart) {
        if (!isRowNumberingCurrentQueryPart()) {
            renderLimitOffsetClause(queryPart);
        }
    }

    // Hibernate 6 support
    @SuppressWarnings({"override"})
    @Override
    public void visitLikePredicate(LikePredicate likePredicate) {
        if (likePredicate.isCaseSensitive()) {
            likePredicate.getMatchExpression().accept(this);
            if (likePredicate.isNegated()) {
                appendSql(" not");
            }
            appendSql(" like ");
            likePredicate.getPattern().accept(this);
            if (likePredicate.getEscapeCharacter() != null) {
                appendSql(" escape ");
                acceptEscapeCharacter(likePredicate);
            }
        } else {
            if (getDialect().supportsCaseInsensitiveLike()) {
                likePredicate.getMatchExpression().accept(this);
                if (likePredicate.isNegated()) {
                    appendSql(" not");
                }
                appendSql(WHITESPACE);
                appendSql(getDialect().getCaseInsensitiveLike());
                appendSql(WHITESPACE);
                likePredicate.getPattern().accept(this);
                if (likePredicate.getEscapeCharacter() != null) {
                    appendSql(" escape ");
                    acceptEscapeCharacter(likePredicate);
                }
            } else {
                renderCaseInsensitiveLikeEmulation(
                        likePredicate.getMatchExpression(),
                        likePredicate.getPattern(),
                        likePredicate.getEscapeCharacter(),
                        likePredicate.isNegated()
                );
            }
        }
    }

    // Hibernate 7 support (parent hook is absent in Hibernate 6)
    @SuppressWarnings({"override"})
    protected void renderLikePredicate(LikePredicate likePredicate) {
        likePredicate.getPattern().accept(this);
        if (likePredicate.getEscapeCharacter() != null) {
            appendSql(" escape ");
            acceptEscapeCharacter(likePredicate);
        }
    }

    private void acceptEscapeCharacter(LikePredicate likePredicate) {
        if (likePredicate.getEscapeCharacter() instanceof QueryLiteral<?> queryLiteral
                && queryLiteral.getLiteralValue() instanceof Character value) {
            appendSql('\'');
            appendSql(value);
            appendSql('\'');
        } else {
            likePredicate.getEscapeCharacter().accept(this);
        }
    }
}
