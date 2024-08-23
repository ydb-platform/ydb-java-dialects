package tech.ydb.data.books.entity;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Data
@Table("books")
public class Book implements Persistable<Long>  {
    @Id
    private Long id;
    private String title;
    private String isbn;
    private long year;

    @MappedCollection(idColumn = "book_id")
    private Set<Review> reviews;
    @MappedCollection(idColumn = "book_id")
    private Set<BookAuthor> authors;

    @Transient
    @EqualsAndHashCode.Exclude
    private boolean isNew;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public Book() {
        this.reviews = new HashSet<>();
        this.authors = new HashSet<>();
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
