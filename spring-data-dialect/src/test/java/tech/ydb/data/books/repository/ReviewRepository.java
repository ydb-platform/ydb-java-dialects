package tech.ydb.data.books.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import tech.ydb.data.books.entity.Review;

/**
 * @author Madiyar Nurgazin
 */
public interface ReviewRepository extends ListCrudRepository<Review, Long>,
        CrudRepository<Review, Long>, PagingAndSortingRepository<Review, Long> {

    List<Review> findByReader(String reader, Pageable pageable);
}
