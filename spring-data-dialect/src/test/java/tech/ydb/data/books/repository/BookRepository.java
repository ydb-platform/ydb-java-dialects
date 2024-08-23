package tech.ydb.data.books.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import tech.ydb.data.books.entity.Book;
import tech.ydb.data.repository.ViewIndex;
import tech.ydb.data.repository.YdbCrudRepository;

/**
 * @author Madiyar Nurgazin
 */
public interface BookRepository extends YdbCrudRepository<Book, Long> {
    @Query("select books.* from books join books_authors on books.id = books_authors.book_id" +
            " join authors on authors.id = books_authors.author_id where name = :author")
    List<Book> findBooksByAuthorName(@Param("author") String author);

    @ViewIndex("isbn_books_index")
    List<Book> findBookByIsbn(String isbn);

    Optional<Book> findBookByTitle(String title);
}
