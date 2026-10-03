package ydb.jimmer.dialect.transaction;


import org.junit.jupiter.api.Test;

public class SnapshotTest extends AbstractTransactionTest {
    @Test
    public void readTest() {
        readTest(yqlClient::snapshotReadOnly);
    }

    @Test
    public void writeTest() {
        writeTest(yqlClient::snapshotReadOnly, true);
    }
}
