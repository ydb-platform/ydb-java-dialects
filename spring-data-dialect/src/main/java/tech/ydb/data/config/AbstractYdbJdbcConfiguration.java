package tech.ydb.data.config;

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
 */
@Configuration
public class AbstractYdbJdbcConfiguration extends AbstractJdbcConfiguration {
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
}
