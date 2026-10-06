package tech.ydb.trino;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import io.airlift.configuration.AbstractConfigurationAwareModule;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.ConnectionFactory;
import io.trino.plugin.jdbc.DriverConnectionFactory;
import io.trino.plugin.jdbc.ForBaseJdbc;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcJoinPushdownSupportModule;
import io.trino.plugin.jdbc.JdbcMetadataFactory;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.credential.CredentialProvider;
import io.trino.spi.connector.ConnectorPageSinkProvider;
import tech.ydb.jdbc.YdbDriver;

import java.util.Properties;

import static com.google.inject.multibindings.OptionalBinder.newOptionalBinder;
import static io.trino.plugin.jdbc.JdbcModule.bindTablePropertiesProvider;

public class YdbClientModule extends AbstractConfigurationAwareModule {

    @Override
    public void setup(Binder binder) {
        install(new JdbcJoinPushdownSupportModule());
        bindJdbcClient(binder);

        newOptionalBinder(binder, QueryBuilder.class)
                .setBinding()
                .to(YdbQueryBuilder.class)
                .in(Scopes.SINGLETON);

        newOptionalBinder(binder, JdbcMetadataFactory.class)
                .setBinding()
                .to(YdbMetadataFactory.class)
                .in(Scopes.SINGLETON);

        bindTablePropertiesProvider(binder, YdbTableProperties.class);
        newOptionalBinder(binder, ConnectorPageSinkProvider.class)
                .setBinding()
                .to(YdbPageSinkProvider.class)
                .in(Scopes.SINGLETON);
        binder.bind(YdbConnector.class).in(Scopes.SINGLETON);
    }

    protected void bindJdbcClient(Binder binder) {
        binder.bind(JdbcClient.class).annotatedWith(ForBaseJdbc.class).to(YdbClient.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    @ForBaseJdbc
    public static ConnectionFactory createConnectionFactory(
            BaseJdbcConfig config,
            CredentialProvider credentialProvider) {
        // JDBC 2.4.1 can close a cached context between lookup and connection registration.
        Properties properties = new Properties();
        properties.setProperty("cacheConnectionsInDriver", "false");
        // InListJdbcPrm bypasses the driver's native SDK-value binding path.
        properties.setProperty("replaceJdbcInByYqlList", "false");
        return DriverConnectionFactory.builder(
                        new YdbDriver(),
                        config.getConnectionUrl(),
                        credentialProvider)
                .setConnectionProperties(properties)
                .build();
    }
}
