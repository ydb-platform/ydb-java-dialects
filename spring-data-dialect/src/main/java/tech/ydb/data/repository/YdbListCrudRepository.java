package tech.ydb.data.repository;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Madiyar Nurgazin
 */
@NoRepositoryBean
public interface YdbListCrudRepository<T, ID> extends YdbRepository<T, ID>, ListCrudRepository<T, ID> {
    <S extends T> List<S> insertAll(Iterable<S> entities);

    <S extends T> List<S> updateAll(Iterable<S> entities);
}
