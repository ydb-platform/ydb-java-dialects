package tech.ydb.trino;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import io.trino.plugin.jdbc.JdbcMetadataConfig;
import io.trino.plugin.jdbc.credential.CredentialProviderModule;
import io.trino.spi.Plugin;
import io.trino.spi.connector.ConnectorFactory;

import java.util.function.Supplier;

import static io.airlift.configuration.ConfigurationAwareModule.combine;
import static io.airlift.configuration.ConfigBinder.configBinder;

public record YdbPlugin(Supplier<Module> module) implements Plugin {
    private static final String NAME = "ydb";

    public YdbPlugin() {
        this(YdbClientModule::new);
    }

    @Override
    public Iterable<ConnectorFactory> getConnectorFactories() {
        return ImmutableList.of(new YdbConnectorFactory(
                NAME,
                () -> combine(
                        new CredentialProviderModule(),
                        binder -> configBinder(binder).bindConfigDefaults(
                                JdbcMetadataConfig.class, config -> config.setComplexJoinPushdownEnabled(false)),
                        module.get()
                )
        ));
    }
}
