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
@Table("authors")
public class Author implements Persistable<Long> {
    @Id
    private Long id;
    private String name;
    @MappedCollection(idColumn = "author_id")
    private Set<BookAuthor> books;

    @Transient
    @EqualsAndHashCode.Exclude
    private boolean isNew;

    public Author() {
        this.books = new HashSet<>();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
