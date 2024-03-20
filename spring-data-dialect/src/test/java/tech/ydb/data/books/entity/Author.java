package tech.ydb.data.books.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
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
}
