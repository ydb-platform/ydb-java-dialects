package tech.ydb.data.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Madiyar Nurgazin
 */
@NoRepositoryBean
public interface YdbRepository<T, ID> extends ListCrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {
}
