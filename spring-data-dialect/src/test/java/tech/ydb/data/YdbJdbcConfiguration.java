package tech.ydb.data;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tech.ydb.data.repository.config.AbstractYdbJdbcConfiguration;
import tech.ydb.data.repository.config.EnableYdbRepositories;

/**
 * @author Madiyar Nurgazin
 */
@Configuration
@EnableYdbRepositories
@EnableJdbcAuditing
public class YdbJdbcConfiguration extends AbstractYdbJdbcConfiguration {
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }
}
