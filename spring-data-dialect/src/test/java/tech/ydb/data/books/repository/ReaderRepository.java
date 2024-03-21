package tech.ydb.data.books.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;
import tech.ydb.data.books.entity.Reader;

/**
 * @author Madiyar Nurgazin
 */
public interface ReaderRepository extends org.springframework.data.repository.ListCrudRepository<Reader, Long>, org.springframework.data.repository.PagingAndSortingRepository<Reader, Long> {

//    @Query("SCAN select * from readers where name = :name")
    List<Reader> findReaderByName(@Param("name") String name);
}
