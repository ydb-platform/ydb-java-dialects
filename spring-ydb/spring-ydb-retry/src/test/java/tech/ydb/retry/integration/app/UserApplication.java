package tech.ydb.retry.integration.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tech.ydb.data.repository.config.AbstractYdbJdbcConfiguration;

@EnableJdbcRepositories
@SpringBootApplication
@Import(AbstractYdbJdbcConfiguration.class)
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
