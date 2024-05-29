package tech.ydb.data.books.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import tech.ydb.data.books.entity.Review;
import tech.ydb.data.repository.YdbListCrudRepository;
import tech.ydb.data.repository.YdbPagingAndSortingRepository;

/**
 * @author Madiyar Nurgazin
 */
public interface ReviewRepository extends YdbListCrudRepository<Review, Long>,
        YdbPagingAndSortingRepository<Review, Long> {
    List<Review> findByReader(String reader, Pageable pageable);
}
