package tech.ydb.data.repository;

import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Madiyar Nurgazin
 */
@NoRepositoryBean
public interface YdbListPagingAndSortingRepository<T, ID> extends YdbRepository<T, ID>,
        ListPagingAndSortingRepository<T, ID> {
}
