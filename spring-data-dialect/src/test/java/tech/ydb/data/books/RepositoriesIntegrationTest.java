package tech.ydb.data.books;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tech.ydb.data.YdbBaseTest;
import tech.ydb.data.books.entity.Author;
import tech.ydb.data.books.entity.Book;
import tech.ydb.data.books.entity.BookAuthor;
import tech.ydb.data.books.entity.Review;
import tech.ydb.data.books.repository.AuthorRepository;
import tech.ydb.data.books.repository.BookRepository;
import tech.ydb.data.books.repository.ReviewRepository;

/**
 * @author Madiyar Nurgazin
 */
public class RepositoriesIntegrationTest extends YdbBaseTest {

    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    public void crudTest() {
        Review review1 = createReview(
                1, 1, "Ivan Ivanov", "Masterpiece!", 10, LocalDateTime.parse("2024-03-19T15:52:26")
        );
        Review review2 = createReview(
                2, 1, "Sergey Petrov", "Complex work, but I liked it", 9, LocalDateTime.parse("2024-03-19T16:14:05")
        );

        List<Book> expected = List.of(
                createBook(1, "War and Peace", "1234", 1869, Set.of(review1, review2), Set.of(new BookAuthor(1, 1))),
                createBook(2, "Anna Karenina", "5678", 1878, Set.of(), Set.of(new BookAuthor(1, 2)))
        );
        Iterable<Book> books = bookRepository.findAll();
        Assertions.assertEquals(expected, books);

        Optional<Book> bookO = bookRepository.findBookByTitle("War and Peace");
        Assertions.assertTrue(bookO.isPresent());

        Review review3 = createReview(
                3, 1, "Madiyar Nurgazin", "Great", 8, LocalDateTime.parse("2024-03-19T20:00:00")
        );

        Book book = bookO.get();
        book.getReviews().add(review3);
        bookRepository.update(book);

        Assertions.assertEquals(3, reviewRepository.count());

        review1.setRating(100);
        review2.setRating(90);
        review3.setRating(80);

        Set<Review> reviews = Set.of(review1, review2, review3);
        reviewRepository.updateAll(reviews);

        bookO = bookRepository.findById(1L);
        Assertions.assertTrue(bookO.isPresent());

        book = bookO.get();
        Assertions.assertEquals(reviews, book.getReviews());

        Author author1 = createAuthor(2, "Author 1");
        Author author2 = createAuthor(3, "Author 2");

        authorRepository.insertAll(List.of(author1, author2));
        Assertions.assertEquals(3, authorRepository.count());

        book = createBook(3, "Title", "Isbn", 2024, Set.of(), Set.of(new BookAuthor(2, 3), new BookAuthor(3, 3)));
        bookRepository.insert(book);


        expected.get(0).setReviews(Set.of(review1, review2, review3));
        books = bookRepository.findBooksByAuthorName("Leo Tolstoy");
        Assertions.assertEquals(expected, books);

        author1.getBooks().add(new BookAuthor(2, 3));
        author2.getBooks().add(new BookAuthor(3, 3));

        List<Author> authors = authorRepository.findAuthorsByBookId(3);
        Assertions.assertEquals(Set.of(author1, author2), Set.copyOf(authors));

        Review review = createReview(4, 3, "Reader", "Text", 5, LocalDateTime.now());
        reviewRepository.insert(review);

        bookRepository.deleteById(3L);

        Assertions.assertFalse(reviewRepository.existsById(review.getId()));
        Assertions.assertTrue(authorRepository.existsById(author1.getId()));

        Optional<Author> author = authorRepository.findById(author2.getId());
        Assertions.assertTrue(author.isPresent());
        Assertions.assertTrue(author.get().getBooks().isEmpty());
    }

    @Test
    public void pagingAndSortingTest() {
        Review review1 = createReview(
                1, 1, "Ivan Ivanov", "Masterpiece!", 10, LocalDateTime.parse("2024-03-19T15:52:26")
        );
        Review review2 = createReview(
                2, 1, "Sergey Petrov", "Complex work, but I liked it", 9, LocalDateTime.parse("2024-03-19T16:14:05")
        );
        Review review3 = createReview(3, 1, "Reader", "Text", 100, LocalDateTime.parse("2024-03-19T21:00:00"));
        Review review4 = createReview(4, 1, "Reader", "Text2", 80, LocalDateTime.parse("2024-03-19T22:00:00"));
        Review review5 = createReview(5, 1, "Reader", "Text3", 75, LocalDateTime.parse("2024-03-19T23:00:00"));
        Review review6 = createReview(6, 1, "Reader", "Text4", 50, LocalDateTime.parse("2024-03-20T00:00:00"));
        reviewRepository.insertAll(List.of(review3, review4, review5, review6));

        Iterable<Review> reviews = reviewRepository.findByReader(
                "Reader", PageRequest.of(0, 2, Sort.by("rating").descending())
        );
        Assertions.assertEquals(List.of(review3, review4), reviews);

        reviews = reviewRepository.findByReader(
                "Reader", PageRequest.of(1, 2, Sort.by("rating").descending())
        );
        Assertions.assertEquals(List.of(review5, review6), reviews);

        reviews = reviewRepository.findAll(Sort.by("created").descending());
        Assertions.assertEquals(List.of(review6, review5, review4, review3, review2, review1), reviews);

        reviews = reviewRepository.findAll(PageRequest.of(0, 3, Sort.by("id"))).getContent();
        Assertions.assertEquals(List.of(review1, review2, review3), reviews);

        reviews = reviewRepository.findAll(PageRequest.ofSize(1)).getContent();
        Assertions.assertEquals(List.of(review1), reviews);

        reviews = reviewRepository.findAll(PageRequest.of(2, 2)).getContent();
        Assertions.assertEquals(List.of(review5, review6), reviews);
    }

    private Review createReview(long id, long bookId, String reader, String text, long rating, LocalDateTime created) {
        Review review = new Review();
        review.setId(id);
        review.setBookId(bookId);
        review.setReader(reader);
        review.setText(text);
        review.setRating(rating);
        review.setCreated(created);
        return review;
    }

    private Book createBook(
            long id, String title, String isbn, long year, Set<Review> reviews, Set<BookAuthor> authors
    ) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setYear(year);
        book.setReviews(reviews);
        book.setAuthors(authors);
        return book;
    }

    private Author createAuthor(long id, String name) {
        Author author = new Author();
        author.setId(id);
        author.setName(name);
        return author;
    }
}
