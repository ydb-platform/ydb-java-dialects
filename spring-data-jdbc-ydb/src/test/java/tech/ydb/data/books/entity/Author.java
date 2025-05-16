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
@Table("authors")
public class Author implements Persistable<Long> {
    @Id
    private Long id;
    private String name;

    @MappedCollection(idColumn = "author_id")
    private Set<BookAuthor> books = new HashSet<>();

    @Transient
    private boolean isNew = false;

    public Author() { }

    public Author(long id, String name) {
        this.id = id;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public Set<BookAuthor> getBooks() {
        return books;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBooks(Set<BookAuthor> books) {
        this.books = books;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        Author other = (Author) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }
}
