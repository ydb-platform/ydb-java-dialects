package tech.ydb.trino;

import com.google.inject.Inject;
import io.airlift.bootstrap.LifeCycleManager;
import io.trino.plugin.base.session.SessionPropertiesProvider;
import io.trino.plugin.jdbc.JdbcConnector;
import io.trino.plugin.jdbc.JdbcTransactionManager;
import io.trino.plugin.jdbc.TablePropertiesProvider;
import io.trino.spi.connector.*;
import io.trino.spi.function.table.ConnectorTableFunction;
import io.trino.spi.procedure.Procedure;

import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static io.trino.spi.connector.ConnectorCapabilities.DEFAULT_COLUMN_VALUE;
import static io.trino.spi.connector.ConnectorCapabilities.NOT_NULL_COLUMN_CONSTRAINT;

@SuppressWarnings("all")
public class YdbConnector extends JdbcConnector {
    @Inject
    public YdbConnector(
            LifeCycleManager lifeCycleManager,
            ConnectorSplitManager jdbcSplitManager,
            ConnectorPageSourceProvider jdbcPageSourceProvider,
            ConnectorPageSinkProvider jdbcPageSinkProvider,
            Optional<ConnectorAccessControl> accessControl,
            Set<Procedure> procedures,
            Set<ConnectorTableFunction> connectorTableFunctions,
            Set<SessionPropertiesProvider> sessionProperties,
            Set<TablePropertiesProvider> tableProperties,
            JdbcTransactionManager transactionManager)
    {
        super(lifeCycleManager, jdbcSplitManager, jdbcPageSourceProvider, jdbcPageSinkProvider,
                accessControl, procedures, connectorTableFunctions, sessionProperties,
                tableProperties, transactionManager);
    }

    @Override
    public Set<ConnectorCapabilities> getCapabilities() {
        return immutableEnumSet(DEFAULT_COLUMN_VALUE, NOT_NULL_COLUMN_CONSTRAINT);
    }
}
