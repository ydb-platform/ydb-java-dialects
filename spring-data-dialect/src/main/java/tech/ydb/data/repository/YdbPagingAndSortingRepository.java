package tech.ydb.data.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Madiyar Nurgazin
 */
@NoRepositoryBean
public interface YdbPagingAndSortingRepository<T, ID> extends YdbRepository<T, ID>, PagingAndSortingRepository<T, ID> {
}
