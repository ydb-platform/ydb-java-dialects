package tech.ydb.data.core;

import java.lang.reflect.Method;

import org.springframework.data.jdbc.repository.query.JdbcQueryMethod;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;

/**
 * Custom {@link JdbcQueryMethod} implementation specific to YDB.
 *
 * @author Mikhail Polivakha
 */
public class YdbQueryMethod extends JdbcQueryMethod {

    public YdbQueryMethod(
      Method method,
      RepositoryMetadata metadata,
      ProjectionFactory factory,
      NamedQueries namedQueries,
      MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext
    ) {
        super(method, metadata, factory, namedQueries, mappingContext);
    }

    @Override
    protected Parameters<?, ?> createParameters(ParametersSource parametersSource) {
        return new YdbQueryParameters(parametersSource);
    }
}
