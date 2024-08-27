package tech.ydb.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tech.ydb.data.repository.config.AbstractYdbJdbcConfiguration;

/**
 * @author Madiyar Nurgazin
 */
@Configuration
@EnableJdbcRepositories
@EnableJdbcAuditing
public class YdbJdbcConfiguration extends AbstractYdbJdbcConfiguration {
}
