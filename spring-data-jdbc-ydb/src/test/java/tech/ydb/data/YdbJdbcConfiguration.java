package tech.ydb.data;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import tech.ydb.data.repository.config.AbstractYdbJdbcConfiguration;

/**
 * @author Madiyar Nurgazin
 * @author Mikhail Polivakha
 */
@Configuration
@EnableJdbcRepositories(
  considerNestedRepositories = true,
  basePackages = "tech.ydb.data"
)
@EnableJdbcAuditing
@EnableAutoConfiguration
public class YdbJdbcConfiguration extends AbstractYdbJdbcConfiguration {}