package tech.ydb.data.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Madiyar Nurgazin
 */
@NoRepositoryBean
public interface YdbCrudRepository<T, ID> extends YdbRepository<T, ID>, CrudRepository<T, ID> {
    <S extends T> Iterable<S> insertAll(Iterable<S> entities);

    <S extends T> Iterable<S> updateAll(Iterable<S> entities);
}
