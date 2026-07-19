package tech.ydb.trino;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import io.airlift.configuration.ConfigBinder;
import io.trino.plugin.base.mapping.IdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.ConnectionFactory;
import io.trino.plugin.jdbc.DriverConnectionFactory;
import io.trino.plugin.jdbc.ForBaseJdbc;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcMetadataFactory;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.credential.CredentialProvider;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import tech.ydb.jdbc.YdbDriver;

import static com.google.inject.multibindings.OptionalBinder.newOptionalBinder;

public class TestingYdbJdbcModule implements Module {

    @Override
    public void configure(Binder binder) {
        ConfigBinder.configBinder(binder).bindConfig(YdbConfig.class);

        newOptionalBinder(binder, JdbcMetadataFactory.class)
                .setBinding()
                .to(YdbMetadataFactory.class)
                .in(Scopes.SINGLETON);

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
        return new TestingYdbJdbcClient(config, connectionFactory, queryBuilder, identifierMapping, remoteQueryModifier);
    }

    @Provides
    @Singleton
    @ForBaseJdbc
    public static ConnectionFactory createConnectionFactory(
            BaseJdbcConfig config,
            CredentialProvider credentialProvider) {
        return DriverConnectionFactory.builder(
                        new YdbDriver(),
                        config.getConnectionUrl(),
                        credentialProvider)
                .build();
    }
}