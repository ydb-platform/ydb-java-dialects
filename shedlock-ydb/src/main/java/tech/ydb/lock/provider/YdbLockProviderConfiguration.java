package tech.ydb.lock.provider;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kirill Kurdyukov
 */

@Configuration
public class YdbLockProviderConfiguration {
    @Bean
    @ConditionalOnBean(DataSource.class)
    public YdbJDBCLockProvider ydbLockProvider(DataSource dataSource) {
        return new YdbJDBCLockProvider(dataSource);
    }
}
