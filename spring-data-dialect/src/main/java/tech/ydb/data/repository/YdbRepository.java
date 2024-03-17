package tech.ydb.data.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Madiyar Nurgazin
 */
@NoRepositoryBean
public interface YdbRepository<T, ID> extends ListCrudRepository<T, ID> {
}
