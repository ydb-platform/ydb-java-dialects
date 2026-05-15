package ydb.jimmer.dialect.transaction;


import org.junit.jupiter.api.Test;

public class StaleTest extends AbstractTransactionTest {
    @Test
    public void readTest() {
        readTest(yqlClient::staleReadOnly);
    }

    @Test
    public void writeTest() {
        writeTest(yqlClient::staleReadOnly, true);
    }
}
