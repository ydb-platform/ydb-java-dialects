package ydb.jimmer.dialect.transaction;


import org.junit.jupiter.api.Test;

public class OnlineConsistentTest extends AbstractTransactionTest {
    @Test
    public void readTest() {
        readTest(yqlClient::onlineConsistentReadOnly);
    }

    @Test
    public void writeTest() {
        writeTest(yqlClient::onlineConsistentReadOnly, true);
    }
}
