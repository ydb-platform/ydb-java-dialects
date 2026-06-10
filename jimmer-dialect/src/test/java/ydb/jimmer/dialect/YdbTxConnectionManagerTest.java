package ydb.jimmer.dialect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import ydb.jimmer.dialect.transaction.YdbTxConnectionManager;

import javax.sql.DataSource;

public class YdbTxConnectionManagerTest extends AbstractTest {
    private final DataSource dataSource = new DriverManagerDataSource(getJdbcURL());

    @Test
    public void testConnectionManager() {
        YdbTxConnectionManager connectionManager = new YdbTxConnectionManager(dataSource);

        Integer value = 2026;
        Assertions.assertEquals(value, connectionManager.execute((con) -> value));
    }
}
