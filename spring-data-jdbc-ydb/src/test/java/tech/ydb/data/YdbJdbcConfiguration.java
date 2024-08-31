package tech.ydb.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tech.ydb.data.repository.config.AbstractYdbJdbcConfiguration;

/**
 * @author Madiyar Nurgazin
 */
@Configuration
@EnableJdbcRepositories
@EnableJdbcAuditing
@Import(AbstractYdbJdbcConfiguration.class)
public class YdbJdbcConfiguration {}