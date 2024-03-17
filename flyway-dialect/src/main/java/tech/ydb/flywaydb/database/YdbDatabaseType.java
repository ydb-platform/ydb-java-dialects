package tech.ydb.flywaydb.database;

import java.sql.Connection;
import java.sql.Types;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.base.BaseDatabaseType;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseType extends BaseDatabaseType {

    private static final String YDB_NAME = "YDB";
    private final static String DRIVER_NAME = "tech.ydb.jdbc.YdbDriver";

    @Override
    public String getName() {
        return YDB_NAME;
    }

    @Override
    public int getNullType() {
        return Types.NULL;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url.startsWith("jdbc:ydb:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        return DRIVER_NAME;
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(
            String databaseProductName,
            String databaseProductVersion,
            Connection connection
    ) {
        return databaseProductName.startsWith(YDB_NAME);
    }

    @Override
    public Database<YdbConnection> createDatabase(
            Configuration configuration,
            JdbcConnectionFactory jdbcConnectionFactory,
            StatementInterceptor statementInterceptor
    ) {
        return new YdbDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public Parser createParser(
            Configuration configuration,
            ResourceProvider resourceProvider,
            ParsingContext parsingContext
    ) {
        return new YdbParser(configuration, parsingContext);
    }
}
