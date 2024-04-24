package tech.ydb.jooq;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.SQLDialect;
import org.jooq.VisitListener;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

import javax.sql.DataSource;
import java.sql.Connection;

public class YdbDslContext extends DefaultDSLContext {

    public static final String YDB_QUOTE = "`";

    public YdbDslContext() {
        this(new DefaultConfiguration());
    }

    public YdbDslContext(Settings settings) {
        this(new DefaultConfiguration().set(settings));
    }

    public YdbDslContext(Connection connection) {
        this(new DefaultConfiguration().set(connection));
    }

    public YdbDslContext(Connection connection, Settings settings) {
        this(new DefaultConfiguration()
                .set(connection)
                .set(settings));
    }

    public YdbDslContext(DataSource datasource) {
        this(new DefaultConfiguration().set(datasource));
    }

    public YdbDslContext(DataSource datasource, Settings settings) {
        this(new DefaultConfiguration().set(datasource).set(settings));
    }

    public YdbDslContext(ConnectionProvider connectionProvider) {
        this(new DefaultConfiguration().set(connectionProvider));
    }

    public YdbDslContext(ConnectionProvider connectionProvider, Settings settings) {
        this(new DefaultConfiguration().set(connectionProvider).set(settings));
    }

    public YdbDslContext(ConnectionFactory connectionFactory) {
        this(new DefaultConfiguration().set(connectionFactory));
    }

    public YdbDslContext(ConnectionFactory connectionFactory, Settings settings) {
        this(new DefaultConfiguration()
                .set(connectionFactory)
                .set(settings));
    }

    public YdbDslContext(Configuration configuration) {
        super(configuration
                .deriveSettings(YdbDslContext::addRequiredParameters)
                .set(SQLDialect.DEFAULT)
                .set(quoteListener()));
    }

    private static Settings addRequiredParameters(Settings settings) {
        return settings
                .withRenderQuotedNames(RenderQuotedNames.NEVER)
                .withRenderSchema(false);
    }

    private static VisitListener quoteListener() {
        return new CustomQuoteListener(YDB_QUOTE);
    }
}
