package tech.ydb.data.repository.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tech.ydb.data.repository.support.SimpleYdbJdbcRepository;

/**
 * @author Madiyar Nurgazin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableJdbcRepositories(repositoryBaseClass = SimpleYdbJdbcRepository.class)
public @interface EnableYdbRepositories {
}
