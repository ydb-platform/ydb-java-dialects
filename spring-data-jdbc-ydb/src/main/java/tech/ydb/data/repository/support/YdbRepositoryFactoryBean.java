package tech.ydb.data.repository.support;

import java.io.Serializable;

import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Custom implementation, specific for YDB Spring Data repositories.
 *
 * @author Mikhail Polivakha
 */
public class YdbRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends JdbcRepositoryFactoryBean<T, S, ID> {

    /**
     * Creates a new {@link JdbcRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public YdbRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return new YdbRepositoryFactory(
          dataAccessStrategy,
          mappingContext,
          converter,
          dialect,
          publisher,
          operations,
          entityCallbacks,
          queryMappingConfiguration
        );
    }
}
