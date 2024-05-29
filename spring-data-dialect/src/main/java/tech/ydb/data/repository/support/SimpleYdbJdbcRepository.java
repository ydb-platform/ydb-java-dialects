package tech.ydb.data.repository.support;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.repository.support.SimpleJdbcRepository;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.transaction.annotation.Transactional;
import tech.ydb.data.repository.YdbCrudRepository;
import tech.ydb.data.repository.YdbPagingAndSortingRepository;

/**
 * @author Madiyar Nurgazin
 */
@Transactional(readOnly = true)
public class SimpleYdbJdbcRepository<T, ID> extends SimpleJdbcRepository<T, ID>
        implements YdbCrudRepository<T, ID>, YdbPagingAndSortingRepository<T, ID> {
    private final JdbcAggregateOperations entityOperations;

    public SimpleYdbJdbcRepository(
            JdbcAggregateOperations entityOperations, PersistentEntity<T, ?> entity, JdbcConverter converter
    ) {
        super(entityOperations, entity, converter);
        this.entityOperations = entityOperations;
    }

    @Transactional
    @Override
    public <S extends T> S insert(S entity) {
        return entityOperations.insert(entity);
    }

    @Transactional
    @Override
    public <S extends T> S update(S entity) {
        return entityOperations.update(entity);
    }

    @Transactional
    @Override
    public <S extends T> Iterable<S> insertAll(Iterable<S> entities) {
        return entityOperations.insertAll(entities);
    }

    @Transactional
    @Override
    public <S extends T> Iterable<S> updateAll(Iterable<S> entities) {
        return entityOperations.updateAll(entities);
    }
}
