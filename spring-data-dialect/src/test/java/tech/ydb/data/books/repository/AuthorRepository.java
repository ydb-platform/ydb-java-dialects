package tech.ydb.data.books.repository;

import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import tech.ydb.data.books.entity.Author;

/**
 * @author Madiyar Nurgazin
 */
public interface AuthorRepository extends ListCrudRepository<Author, Long> {
    @Query("select authors.* from authors join books_authors on authors.id = books_authors.author_id" +
            " where books_authors.book_id = :bookId")
    List<Author> findAuthorsByBookId(@Param("bookId") long bookId);
}
