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
@Table("readers")
public class Reader {
    @Id
    private long id;
    private String name;

    @MappedCollection(idColumn = "reader_id")
    private Set<ReviewRef> reviews;

    public Reader() {
        this.reviews = new HashSet<>();
    }
}
