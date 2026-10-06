package tech.ydb.trino;

import io.trino.spi.connector.Connector;
import io.trino.testing.TestingConnectorContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static com.google.common.collect.Iterables.getOnlyElement;
import static io.trino.plugin.jdbc.JoinPushdownStrategy.AUTOMATIC;
import static io.trino.spi.connector.ConnectorCapabilities.NOT_NULL_COLUMN_CONSTRAINT;
import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbPlugin {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testConnectorBootstrap(boolean testingClient) {
        YdbPlugin plugin = new YdbPlugin(testingClient ? TestingYdbJdbcModule::new : YdbClientModule::new);
        var factory = getOnlyElement(plugin.getConnectorFactories());
        for (int index = 0; index < 2; index++) {
            Connector connector = factory.create("ydb_bootstrap_" + index, Map.of(
                    "connection-url", "jdbc:ydb:grpc://127.0.0.1:2136/local",
                    "statistics.enabled", Boolean.toString(index == 0)), new TestingConnectorContext());
            try {
                assertThat(connector.getPageSinkProvider()).isInstanceOf(YdbPageSinkProvider.class);
                assertThat(connector.getCapabilities()).contains(NOT_NULL_COLUMN_CONSTRAINT);
                assertThat(connector.getSessionProperties())
                        .filteredOn(property -> property.getName().equals("join_pushdown_enabled"))
                        .extracting(property -> (Object) property.getDefaultValue())
                        .containsExactly(true);
                assertThat(connector.getSessionProperties())
                        .filteredOn(property -> property.getName().equals("join_pushdown_strategy"))
                        .extracting(property -> (Object) property.getDefaultValue())
                        .containsExactly(AUTOMATIC);
                assertThat(connector.getSessionProperties())
                        .filteredOn(property -> property.getName().equals("complex_join_pushdown_enabled"))
                        .extracting(property -> (Object) property.getDefaultValue())
                        .containsExactly(false);
            }
            finally {
                connector.shutdown();
            }
        }
    }
}
