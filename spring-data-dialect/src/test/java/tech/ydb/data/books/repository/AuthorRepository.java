package tech.ydb.data.books.repository;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import tech.ydb.data.books.entity.Author;

/**
 * @author Madiyar Nurgazin
 */
public interface AuthorRepository extends ListCrudRepository<Author, Long> {
    List<Author> findAuthorsByName(String name);
}
