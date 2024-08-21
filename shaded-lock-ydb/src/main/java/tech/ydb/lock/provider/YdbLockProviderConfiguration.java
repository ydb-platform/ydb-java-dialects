package tech.ydb.lock.provider;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.ydb.jdbc.YdbConnection;

/**
 * @author Kirill Kurdyukov
 */

@Configuration
public class YdbLockProviderConfiguration {
    @Bean
    public YdbCoordinationServiceLockProvider ydbLockProvider(DataSource dataSource) throws SQLException {
        return new YdbCoordinationServiceLockProvider(dataSource.getConnection().unwrap(YdbConnection.class));
    }
}
