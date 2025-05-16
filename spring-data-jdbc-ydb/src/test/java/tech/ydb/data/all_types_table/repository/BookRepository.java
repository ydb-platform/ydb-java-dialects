package tech.ydb.data.all_types_table.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import tech.ydb.data.books.entity.Book;
import tech.ydb.data.repository.ViewIndex;

/**
 * @author Madiyar Nurgazin
 */
public interface BookRepository extends CrudRepository<Book, Long> {
    @Query("select books.* from books join books_authors on books.id = books_authors.book_id" +
            " join authors on authors.id = books_authors.author_id where name = :author")
    List<Book> findBooksByAuthorName(@Param("author") String author);

    @ViewIndex(indexName = "isbn_books_index", tableName = "books")
    List<Book> findBookByIsbn(String isbn);

    Optional<Book> findBookByTitle(String title);
}
