package tech.ydb.data.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * @author Madiyar Nurgazin
 */
@NoRepositoryBean
public interface YdbRepository<T, ID> extends Repository<T, ID> {
    <S extends T> S insert(S entity);

    <S extends T> S update(S entity);
}
