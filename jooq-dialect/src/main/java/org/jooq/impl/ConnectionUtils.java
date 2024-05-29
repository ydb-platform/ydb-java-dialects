package org.jooq.impl;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.ConnectionProvider;
import org.jooq.tools.jdbc.JDBCUtils;

import java.sql.Connection;

public final class ConnectionUtils {
    private ConnectionUtils() {
        throw new UnsupportedOperationException();
    }

    public static ConnectionProvider closeableProvider(Connection connection) {
        return new DefaultCloseableConnectionProvider(connection);
    }

    public static void closeConnectionProvider(ConnectionProvider connectionProvider) {
        if (connectionProvider instanceof DefaultCloseableConnectionProvider dcp) {
            JDBCUtils.safeClose(dcp.connection);
            dcp.connection = null;
        }
    }

    public static void closeConnectionFactory(ConnectionFactory connectionFactory) {
        if (connectionFactory instanceof DefaultConnectionFactory dcf) {
            if (dcf.finalize) {
                R2DBC.blockWrappingExceptions(dcf.connection.close());
                dcf.connection = null;
            }
        }
    }
}
