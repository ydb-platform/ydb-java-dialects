package ydb.jimmer.dialect.transaction;


import org.junit.jupiter.api.Test;

public class OnlineInconsistentTest extends AbstractTransactionTest {
    @Test
    public void readTest() {
        readTest(yqlClient::onlineInconsistentReadOnly);
    }

    @Test
    public void writeTest() {
        writeTest(yqlClient::onlineInconsistentReadOnly, true);
    }
}
