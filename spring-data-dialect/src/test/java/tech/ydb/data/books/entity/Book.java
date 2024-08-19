package tech.ydb.data.books.entity;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Data
@Table("books")
public class Book {
    @Id
    private long id;
    private String title;
    private String isbn;
    private long year;

    @MappedCollection(idColumn = "book_id")
    private Set<Review> reviews;
    @MappedCollection(idColumn = "book_id")
    private Set<BookAuthor> authors;

    public Book() {
        this.reviews = new HashSet<>();
        this.authors = new HashSet<>();
    }
}
