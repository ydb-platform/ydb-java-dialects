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
@Table("authors")
public class Author {
    @Id
    private long id;
    private String name;
    @MappedCollection(idColumn = "author_id")
    private Set<BookAuthor> books;

    public Author() {
        this.books = new HashSet<>();
    }
}
