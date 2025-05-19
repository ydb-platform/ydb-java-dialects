package tech.ydb.data.books.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Table("books")
public class Book implements Persistable<Long> {
    @Id
    private Long id;
    private String title;
    private String isbn;
    private long year;

    @Transient
    private boolean isNew = false;

    @MappedCollection(idColumn = "book_id")
    private Set<Review> reviews = new HashSet<>();
    @MappedCollection(idColumn = "book_id")
    private Set<BookAuthor> authors = new HashSet<>();

    public Book() { }

    public Book(long id, String title, String isbn, long year) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
        this.year = year;
        this.isNew = true;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getTitle() {
        return title;
    }

    public String getIsbn() {
        return isbn;
    }

    public long getYear() {
        return year;
    }

    public Set<Review> getReviews() {
        return reviews;
    }

    public Set<BookAuthor> getAuthors() {
        return authors;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setYear(long year) {
        this.year = year;
    }

    public void setReviews(Set<Review> reviews) {
        this.reviews = reviews;
    }

    public void setAuthors(Set<BookAuthor> authors) {
        this.authors = authors;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, isbn, year);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        Book other = (Book) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(title, other.title)
                && Objects.equals(isbn, other.isbn)
                && year == other.year;
    }
}
