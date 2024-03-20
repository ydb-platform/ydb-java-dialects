package tech.ydb.data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tech.ydb.data.all_types_table.entity.AllTypesEntity;
import tech.ydb.data.books.entity.Author;
import tech.ydb.data.books.entity.Book;
import tech.ydb.data.books.entity.Reader;
import tech.ydb.data.config.AbstractYdbJdbcConfiguration;

/**
 * @author Madiyar Nurgazin
 */
@Configuration
@EnableJdbcRepositories
@EnableJdbcAuditing
public class YdbJdbcConfiguration extends AbstractYdbJdbcConfiguration {
    private final AtomicInteger allTypesEntityId = new AtomicInteger(3);
    private final AtomicLong authorId = new AtomicLong(1);
    private final AtomicLong bookId = new AtomicLong(2);
    private final AtomicLong readerId = new AtomicLong(2);

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    BeforeConvertCallback<AllTypesEntity> beforeAllTypesEntityConvertCallback() {
        return (entity) -> {
            if (entity.getId() == 0) {
                entity.setId(allTypesEntityId.incrementAndGet());
            }
            return entity;
        };
    }

    @Bean
    BeforeConvertCallback<Author> beforeAuthorConvertCallback() {
        return (author) -> {
            if (author.getId() == 0) {
                author.setId(authorId.incrementAndGet());
            }
            return author;
        };
    }

    @Bean
    BeforeConvertCallback<Book> beforeBookConvertCallback() {
        return (book) -> {
            if (book.getId() == 0) {
                book.setId(bookId.incrementAndGet());
            }
//            book.getReviews().forEach(review -> review.set);
            return book;
        };
    }

    @Bean
    BeforeConvertCallback<Reader> beforeReaderConvertCallback() {
        return (reader) -> {
            if (reader.getId() == 0) {
                reader.setId(readerId.incrementAndGet());
            }
//            book.getReviews().forEach(review -> review.set);
            return reader;
        };
    }
}
