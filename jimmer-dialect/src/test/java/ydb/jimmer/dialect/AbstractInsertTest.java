package ydb.jimmer.dialect;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.MutationResult;
import org.junit.jupiter.api.Assertions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Consumer;

public abstract class AbstractInsertTest extends AbstractTest {
    protected void executeAndExpect(Executable<? extends MutationResult> query, Consumer<QueryTestContext> block) {
        MutationResult result = null;
        Throwable throwable = null;
        try (Connection connection = DriverManager.getConnection(getJdbcURL())) {
            connection.setAutoCommit(false);
            try {
                result = query.execute(connection);
            } catch (Throwable ex) {
                throwable = ex;
            } finally {
                connection.rollback();
            }
        } catch (SQLException e) {
            Assertions.fail("Database threw an exception: " + e.getMessage());
        }

        block.accept(new QueryTestContext(executor.getLogs(), result, throwable));
    }
}
