package tech.ydb.trino;

import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.credential.StaticCredentialProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestYdbNamespaceConfig {
    @Test
    void testRejectPrefixRootOverride() {
        for (String prefix : new String[] {"sales", "../other", "%2E%2E%2Fother", "/absolute"}) {
            BaseJdbcConfig config = new BaseJdbcConfig()
                    .setConnectionUrl("jdbc:ydb:grpc://localhost:2136/local?usePrefixPath=" + prefix);
            assertThatThrownBy(() -> YdbClientModule.createConnectionFactory(config,
                    new StaticCredentialProvider(Optional.empty(), Optional.empty())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("usePrefixPath is unsupported");
        }
    }

    @Test
    void testDatabaseRootConfiguration() throws Exception {
        try (var factory = YdbClientModule.createConnectionFactory(
                new BaseJdbcConfig().setConnectionUrl("jdbc:ydb:grpc://localhost:2136/local"),
                new StaticCredentialProvider(Optional.empty(), Optional.empty()))) {
            // Factory construction must not require a live database.
        }
    }
}
