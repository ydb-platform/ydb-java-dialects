package tech.ydb.data.repository.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.jdbc.repository.config.DialectResolver;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.lang.Nullable;
import tech.ydb.data.core.dialect.YdbDialect;

/**
 * @author Madiyar Nurgazin
 */
public class YdbDialectProvider extends DialectResolver.DefaultDialectProvider {

    @Override
    public Optional<Dialect> getDialect(JdbcOperations operations) {
        Optional<Dialect> ydbDialect = Optional.ofNullable(
                operations.execute((ConnectionCallback<Dialect>) YdbDialectProvider::getDialect)
        );

        if (ydbDialect.isPresent()) {
            return ydbDialect;
        }

        return super.getDialect(operations);
    }

    @Nullable
    private static Dialect getDialect(Connection connection) throws SQLException {
        if ("ydb".contains(connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ENGLISH))) {
            return YdbDialect.INSTANCE;
        }

        return null;
    }
}
