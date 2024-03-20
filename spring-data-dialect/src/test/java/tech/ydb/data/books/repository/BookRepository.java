package tech.ydb.data.books.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import tech.ydb.data.books.entity.Book;

/**
 * @author Madiyar Nurgazin
 */
public interface BookRepository extends ListCrudRepository<Book, Long>, PagingAndSortingRepository<Book, Long> {
    @Query("select books.* from books join books_authors on books.id = books_authors.book_id join authors on authors.id = books_authors.author_id where name = :author")
    List<Book> findBooksByAuthorName(@Param("author") String author);

    List<Book> findBookByTitle(String title);

//    List<Book> find
}
