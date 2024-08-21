package tech.ydb.lock.provider;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.jdbc.YdbConnection;
import tech.ydb.jdbc.YdbDriver;

/**
 * @author Kirill Kurdyukov
 */

@Configuration
@EnableConfigurationProperties(YdbLockProperties.class)
public class YdbLockProviderConfiguration {

    @Configuration
    @ConditionalOnClass(YdbDriver.class)
    public static class YdbLockProviderDataSourceConfiguration {

        @Bean
        @ConditionalOnBean(DataSource.class)
        public CoordinationClient coordinationClient(DataSource dataSource) throws SQLException {
            try (var ydbConnection = dataSource.getConnection().unwrap(YdbConnection.class)) {

                return CoordinationClient.newClient(ydbConnection.getCtx().getGrpcTransport());
            }
        }
    }

    @Bean
    public YdbLockProvider ydbLockProvider(CoordinationClient coordinationClient,
                                           YdbLockProperties ydbLockProperties) {
        var ydbLockProvider = new YdbLockProvider(coordinationClient, ydbLockProperties);

        ydbLockProvider.init();

        return ydbLockProvider;
    }
}
