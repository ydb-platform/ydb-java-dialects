package tech.ydb.data.books.entity;

import java.time.LocalDateTime;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Data
@Table("reviews")
public class Review {
    @Id
    private long id;
    private long bookId;
    private String reader;
    private String text;
    private long rating;
    private LocalDateTime created;

    public Review() {
        this.created = LocalDateTime.now();
    }
}
