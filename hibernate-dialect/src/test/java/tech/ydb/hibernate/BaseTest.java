package tech.ydb.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.function.Consumer;

/**
 * @author Kirill Kurdyukov
 */
public abstract class BaseTest {

    @RegisterExtension
    private static final YdbHelperExtension YDB_HELPER_EXTENSION = new YdbHelperExtension();

    protected static SessionFactory SESSION_FACTORY;

    protected static String jdbcUrl() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(YDB_HELPER_EXTENSION.useTls() ? "grpcs://" : "grpc://")
                .append(YDB_HELPER_EXTENSION.endpoint())
                .append(YDB_HELPER_EXTENSION.database());

        if (YDB_HELPER_EXTENSION.authToken() != null) {
            jdbc.append("?").append("token=").append(YDB_HELPER_EXTENSION.authToken());
        }

        return jdbc.toString();
    }

    protected static void inTransaction(Consumer<Session> work) {
        SESSION_FACTORY.inTransaction(work);
    }
}
