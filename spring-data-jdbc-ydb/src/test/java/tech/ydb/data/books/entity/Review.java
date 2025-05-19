package tech.ydb.data.books.entity;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Table("reviews")
public class Review implements Persistable<Long> {
    @Id
    private Long id;
    private long bookId;
    private String reader;
    private String text;
    private long rating;
    private Instant created;

    @Transient
    private boolean isNew = false;

    public Review() { }

    public Review(long id, long bookId, String reader, String text, long rating, Instant created) {
        this.id = id;
        this.bookId = bookId;
        this.reader = reader;
        this.text = text;
        this.rating = rating;
        this.created = created;
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

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public long getBookId() {
        return bookId;
    }

    public String getReader() {
        return reader;
    }

    public String getText() {
        return text;
    }

    public long getRationg() {
        return rating;
    }

    public Instant getCreated() {
        return created;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public void setReader(String reader) {
        this.reader = reader;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setRating(long rating) {
        this.rating = rating;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bookId, reader, text, rating, created);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        Review other = (Review) obj;
        return Objects.equals(id, other.id)
                && bookId == other.bookId
                && Objects.equals(reader, other.reader)
                && Objects.equals(text, other.text)
                && rating == other.rating
                && Objects.equals(created, other.created);
    }
}
