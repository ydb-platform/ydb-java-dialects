package tech.ydb.data.repository.support;

import java.lang.reflect.Method;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.repository.QueryMappingConfiguration;
import org.springframework.data.jdbc.repository.query.JdbcQueryMethod;
import org.springframework.data.jdbc.repository.query.PartTreeJdbcQuery;
import org.springframework.data.jdbc.repository.query.RowMapperFactory;
import org.springframework.data.jdbc.repository.query.StringBasedJdbcQuery;
import org.springframework.data.jdbc.repository.support.BeanFactoryAwareRowMapperFactory;
import org.springframework.data.jdbc.repository.support.JdbcQueryLookupStrategy;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.CachingValueExpressionDelegate;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.lang.Nullable;

import tech.ydb.data.core.YdbQueryMethod;

/**
 * Custom {@link JdbcRepositoryFactory repository factory} to allow for query tuning.
 *
 * @author Mikhail Polivakha
 */
public class YdbRepositoryFactory extends JdbcRepositoryFactory {

    private final EntityCallbacks entityCallbacks;
    private final QueryMappingConfiguration queryMappingConfiguration;

    private static final Log LOG = LogFactory.getLog(JdbcQueryLookupStrategy.class);

    /**
     * Creates a new {@link JdbcRepositoryFactory} for the given {@link DataAccessStrategy},
     * {@link RelationalMappingContext} and {@link ApplicationEventPublisher}.
     *
     * @param dataAccessStrategy must not be {@literal null}.
     * @param context            must not be {@literal null}.
     * @param converter          must not be {@literal null}.
     * @param dialect            must not be {@literal null}.
     * @param publisher          must not be {@literal null}.
     * @param operations         must not be {@literal null}.
     */
    public YdbRepositoryFactory(
      DataAccessStrategy dataAccessStrategy,
      RelationalMappingContext context,
      JdbcConverter converter,
      Dialect dialect,
      ApplicationEventPublisher publisher,
      NamedParameterJdbcOperations operations,
      EntityCallbacks entityCallbacks,
      QueryMappingConfiguration queryMappingConfiguration
    ) {
        super(dataAccessStrategy, context, converter, dialect, publisher, operations);

        this.entityCallbacks = entityCallbacks;
        this.queryMappingConfiguration = queryMappingConfiguration;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key, ValueExpressionDelegate valueExpressionDelegate) {
        return Optional.of(
          new YdbQueryLookupStrategy(
            publisher,
            entityCallbacks,
            context,
            converter,
            dialect,
            beanFactory,
            queryMappingConfiguration,
            operations,
            new CachingValueExpressionDelegate(valueExpressionDelegate)
          )
        );
    }

    static class YdbQueryLookupStrategy extends JdbcQueryLookupStrategy {

        private final RowMapperFactory rowMapperFactory;

        public YdbQueryLookupStrategy(
          ApplicationEventPublisher publisher, @Nullable EntityCallbacks callbacks,
          RelationalMappingContext context, JdbcConverter converter, Dialect dialect, BeanFactory beanFactory,
          QueryMappingConfiguration queryMappingConfiguration, NamedParameterJdbcOperations operations,
          ValueExpressionDelegate delegate
        ) {
            super(publisher, callbacks, context, converter, dialect, queryMappingConfiguration, operations, delegate);

            this.rowMapperFactory = new BeanFactoryAwareRowMapperFactory(context, converter, queryMappingConfiguration, callbacks, publisher, beanFactory);
        }

        @Override
        public RepositoryQuery resolveQuery(
          Method method,
          RepositoryMetadata repositoryMetadata,
          ProjectionFactory projectionFactory,
          NamedQueries namedQueries
        ) {
            JdbcQueryMethod queryMethod = new YdbQueryMethod(method, repositoryMetadata, projectionFactory, namedQueries, getMappingContext());

            if (namedQueries.hasQuery(queryMethod.getNamedQueryName()) || queryMethod.hasAnnotatedQuery()) {

                if (queryMethod.hasAnnotatedQuery() && queryMethod.hasAnnotatedQueryName()) {
                    LOG.warn(String.format(
                      "Query method %s is annotated with both, a query and a query name; Using the declared query", method));
                }

                String queryString = evaluateTableExpressions(repositoryMetadata, queryMethod.getRequiredQuery());

                return new StringBasedJdbcQuery(queryString, queryMethod, getOperations(), rowMapperFactory, getConverter(),
                  delegate);
            } else {
                return new PartTreeJdbcQuery(getMappingContext(), queryMethod, getDialect(), getConverter(), getOperations(), rowMapperFactory);
            }
        }
    }
}
