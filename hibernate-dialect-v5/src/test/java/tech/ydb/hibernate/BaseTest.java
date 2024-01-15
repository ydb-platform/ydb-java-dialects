package tech.ydb.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.hibernate.dialect.YdbDialect;
import tech.ydb.jdbc.YdbDriver;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.function.Consumer;

/**
 * @author Kirill Kurdyukov
 */
public abstract class BaseTest {

    @RegisterExtension
    private static final YdbHelperExtension YDB_HELPER_EXTENSION = new YdbHelperExtension();

    protected static SessionFactory SESSION_FACTORY;

    protected static Configuration basedConfiguration() {
        return new Configuration()
                .setProperty(AvailableSettings.DRIVER, YdbDriver.class.getName())
                .setProperty(AvailableSettings.DIALECT, YdbDialect.class.getName())
                .setProperty(AvailableSettings.HBM2DDL_AUTO, "create")
                .setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString())
                .setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString())
                .setProperty(AvailableSettings.HIGHLIGHT_SQL, Boolean.TRUE.toString());
    }

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
        Session session = SESSION_FACTORY.openSession();

        Transaction transaction = session.getTransaction();
        try {
            transaction.begin();
            work.accept(session);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            throw e;
        } finally {
            session.close();
        }
    }
}
