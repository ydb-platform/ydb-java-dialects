package tech.ydb.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import tech.ydb.data.repository.config.AbstractYdbJdbcConfiguration;
import tech.ydb.data.repository.support.YdbRepositoryFactoryBean;

/**
 * @author Madiyar Nurgazin
 * @author Mikhail Polivakha
 */
@Configuration
@EnableJdbcRepositories(
  considerNestedRepositories = true,
  basePackages = "tech.ydb.data",
  repositoryFactoryBeanClass = YdbRepositoryFactoryBean.class
)
@EnableJdbcAuditing
@Import(AbstractYdbJdbcConfiguration.class)
public class YdbJdbcConfiguration {

}
