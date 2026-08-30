package tech.ydb.trino;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.trino.plugin.jdbc.ConnectionFactory;
import io.trino.spi.connector.ConnectorSession;
import jakarta.annotation.PreDestroy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class YdbHikariConnectionFactory implements ConnectionFactory {
    private final HikariDataSource dataSource;

    public YdbHikariConnectionFactory(String connectionUrl, Properties connectionProperties) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("ydb-trino-pool");
        config.setDriverClassName("tech.ydb.jdbc.YdbDriver");
        config.setJdbcUrl(connectionUrl);

        for (String name : connectionProperties.stringPropertyNames()) {
            config.addDataSourceProperty(name, connectionProperties.getProperty(name));
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(3);

        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_800_000);
        config.setKeepaliveTime(30_000);
        config.setConnectionTimeout(5_000);
        config.setLeakDetectionThreshold(60_000);

        config.setAutoCommit(true);
        config.setReadOnly(false);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection openConnection(ConnectorSession session) throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    @PreDestroy
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
