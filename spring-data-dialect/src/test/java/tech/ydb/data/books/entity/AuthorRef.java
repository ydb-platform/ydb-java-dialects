package tech.ydb.data.books.entity;

import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Table("books_authors")
public record AuthorRef(long authorId) {
}
