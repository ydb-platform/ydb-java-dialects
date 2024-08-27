package tech.ydb.data.books.entity;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Data
@Table("reviews")
public class Review implements Persistable<Long> {
    @Id
    private Long id;
    private long bookId;
    private String reader;
    private String text;
    private long rating;
    private Instant created;

    public Review() {
        this.created = Instant.now();
    }

    @Transient
    @EqualsAndHashCode.Exclude
    private boolean isNew;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
