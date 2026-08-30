package tech.ydb.trino;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import io.trino.plugin.base.mapping.IdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.ConnectionFactory;
import io.trino.plugin.jdbc.ForBaseJdbc;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcMetadataFactory;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.credential.CredentialProvider;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;

import java.util.Properties;

import static com.google.inject.multibindings.OptionalBinder.newOptionalBinder;

public class YdbClientModule implements Module {

    @Override
    public void configure(Binder binder) {
        newOptionalBinder(binder, JdbcMetadataFactory.class)
                .setBinding()
                .to(YdbMetadataFactory.class)
                .in(Scopes.SINGLETON);

        newOptionalBinder(binder, QueryBuilder.class)
                .setBinding()
                .to(YdbQueryBuilder.class)
                .in(Scopes.SINGLETON);

        binder.bind(YdbPageSinkProvider.class).in(Scopes.SINGLETON);
        binder.bind(YdbConnector.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    @ForBaseJdbc
    public JdbcClient provideJdbcClient(
            BaseJdbcConfig config,
            ConnectionFactory connectionFactory,
            QueryBuilder queryBuilder,
            IdentifierMapping identifierMapping,
            RemoteQueryModifier remoteQueryModifier) {
        return new YdbClient(config, connectionFactory, queryBuilder, identifierMapping, remoteQueryModifier);
    }

    @Provides
    @Singleton
    @ForBaseJdbc
    public static ConnectionFactory createConnectionFactory(
            BaseJdbcConfig config,
            CredentialProvider credentialProvider
    ) {
        Properties connectionProperties = new Properties();
        // Avoid the YDB JDBC 2.3.18 shared-context close/register race under concurrent connections.
        connectionProperties.setProperty("cacheConnectionsInDriver", "false");
        return new YdbHikariConnectionFactory(config.getConnectionUrl(), connectionProperties);
    }
}
