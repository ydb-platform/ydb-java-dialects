package tech.ydb.data.books.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import tech.ydb.data.books.entity.Author;
import tech.ydb.data.repository.ViewIndex;

/**
 * @author Madiyar Nurgazin
 */
public interface AuthorRepository extends CrudRepository<Author, Long> {
    @Query("select authors.* from authors join books_authors on authors.id = books_authors.author_id" +
            " where books_authors.book_id = :bookId")
    List<Author> findAuthorsByBookId(@Param("bookId") long bookId);

    @ViewIndex(indexName = "name_authors_index", tableName = "authors")
    List<Author> findAuthorByName(@Param("name") String name);
}
