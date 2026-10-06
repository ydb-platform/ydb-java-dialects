package tech.ydb.data.repository.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DefaultJdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.JdbcArrayColumns;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import tech.ydb.data.core.convert.YdbDataAccessStrategy;
import tech.ydb.data.core.convert.YdbMappingJdbcConverter;

/**
 * @author Madiyar Nurgazin
 * @author Mikhail Polivakha
 */
@Configuration
@Import(JdbcRepositoryBeanPostProcessor.class)
public class AbstractYdbJdbcConfiguration extends AbstractJdbcConfiguration {

    // Spring Boot 4 support
    @SuppressWarnings({"override", "removal"})
    public JdbcConverter jdbcConverter(
            JdbcMappingContext mappingContext,
            NamedParameterJdbcOperations operations,
            @Lazy RelationResolver relationResolver,
            JdbcCustomConversions conversions,
            JdbcDialect dialect
    ) {
        DefaultJdbcTypeFactory jdbcTypeFactory = new DefaultJdbcTypeFactory(
                operations.getJdbcOperations(), JdbcArrayColumns.Unsupported.INSTANCE
        );

        return new YdbMappingJdbcConverter(mappingContext, relationResolver, conversions, jdbcTypeFactory);
    }

    // Spring Boot 3 support
    @SuppressWarnings({"override", "removal"})
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

    /**
     * Decorates the framework-provided {@code dataAccessStrategyBean} to work around a Spring Data JDBC
     * limitation with composite (embedded) {@code @Id} entities that makes {@code save()} of an existing entity
     * fail on YDB with "Cannot update primary key column" - see {@link YdbDataAccessStrategy} for details.
     * <p>
     * This is a separate, {@link Primary @Primary} bean rather than an override of
     * {@code dataAccessStrategyBean(...)} because that method's parameter type ({@code Dialect} vs.
     * {@code JdbcDialect}) differs between the Spring Data JDBC versions this module supports, while
     * {@code DataAccessStrategy} and {@code Dialect} themselves are stable across all of them.
     */
    @Bean
    @Primary
    public DataAccessStrategy ydbDataAccessStrategy(@Qualifier("dataAccessStrategyBean") DataAccessStrategy defaultStrategy,
            NamedParameterJdbcOperations operations, JdbcConverter jdbcConverter, Dialect dialect) {

        return new YdbDataAccessStrategy(defaultStrategy, operations, jdbcConverter, dialect);
    }
}
