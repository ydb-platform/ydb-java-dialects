package tech.ydb.jooq.impl;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.ConnectionProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.ConnectionUtils;
import tech.ydb.jooq.CloseableYdbDSLContext;

/**
 * An extension of {@link YdbDSLContextImpl} that implements also the
 * {@link CloseableYdbDSLContext} contract.
 */
public class DefaultCloseableYdbDSLContext extends YdbDSLContextImpl implements CloseableYdbDSLContext {
    public DefaultCloseableYdbDSLContext(ConnectionProvider connectionProvider, Settings settings) {
        super(connectionProvider, settings);
    }

    public DefaultCloseableYdbDSLContext(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    public DefaultCloseableYdbDSLContext(ConnectionFactory connectionFactory, Settings settings) {
        super(connectionFactory, settings);
    }

    public DefaultCloseableYdbDSLContext(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void close() {
        ConnectionProvider cp = configuration().connectionProvider();
        ConnectionFactory cf = configuration().connectionFactory();

        ConnectionUtils.closeConnectionProvider(cp);
        ConnectionUtils.closeConnectionFactory(cf);
    }
}
