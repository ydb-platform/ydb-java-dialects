package tech.ydb.hibernate.dialect.translator;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * @author Kirill Kurdyukov
 */
public class YdbSqlAstTranslatorFactory extends StandardSqlAstTranslatorFactory {

    public static final YdbSqlAstTranslatorFactory YDB_SQL_AST_TRANSLATOR_FACTORY = new YdbSqlAstTranslatorFactory();

    @Override
    protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
            SessionFactoryImplementor sessionFactory,
            Statement statement
    ) {
        return new YdbSqlAstTranslator<>(sessionFactory, statement);
    }
}
