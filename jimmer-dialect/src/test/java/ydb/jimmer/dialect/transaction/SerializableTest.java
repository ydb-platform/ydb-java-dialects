package ydb.jimmer.dialect.transaction;


import org.junit.jupiter.api.Test;

public class SerializableTest extends AbstractTransactionTest {
    @Test
    public void readTest() {
        readTest(yqlClient::serializableReadWrite);
    }

    @Test
    public void writeTest() {
        writeTest(yqlClient::serializableReadWrite, false);
    }
}
