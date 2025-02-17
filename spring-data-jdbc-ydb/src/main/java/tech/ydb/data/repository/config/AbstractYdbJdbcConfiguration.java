package tech.ydb.data.repository.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jdbc.core.convert.DefaultJdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.JdbcArrayColumns;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import tech.ydb.data.core.convert.YdbMappingJdbcConverter;

/**
 * @author Madiyar Nurgazin
 * @author Mikhail Polivakha
 */
@Configuration(proxyBeanMethods = true)
public class AbstractYdbJdbcConfiguration extends AbstractJdbcConfiguration {

    @Bean
    public YdbDialectProvider ydbDialectProvider() {
        return new YdbDialectProvider();
    }

    @Override
    public JdbcConverter jdbcConverter(
            JdbcMappingContext mappingContext,
            NamedParameterJdbcOperations operations,
            @Lazy RelationResolver relationResolver,
            JdbcCustomConversions conversions,
            Dialect dialect
    ) {
        DefaultJdbcTypeFactory jdbcTypeFactory = new DefaultJdbcTypeFactory(
                operations.getJdbcOperations(), JdbcArrayColumns.Unsupported.INSTANCE
        );

        return new YdbMappingJdbcConverter(mappingContext, relationResolver, conversions, jdbcTypeFactory);
    }

    @Override
    public Dialect jdbcDialect(NamedParameterJdbcOperations operations) {
        return ydbDialectProvider()
          .getDialect(operations.getJdbcOperations())
          .orElseThrow(() -> new IllegalStateException(String.format("Cannot determine a dialect for %s; Please provide a Dialect", operations)));
    }
}
