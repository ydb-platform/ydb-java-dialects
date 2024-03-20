package tech.ydb.data.books.entity;

import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@Table("readers_reviews")
public record ReviewRef(long reviewId) {
}
