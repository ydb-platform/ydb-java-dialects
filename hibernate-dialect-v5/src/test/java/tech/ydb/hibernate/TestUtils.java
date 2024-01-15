package tech.ydb.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import tech.ydb.hibernate.dialect.YdbDialect;
import tech.ydb.jdbc.YdbDriver;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.function.Consumer;

/**
 * @author Kirill Kurdyukov
 */
public class TestUtils {

    public static SessionFactory SESSION_FACTORY;

    public static Configuration basedConfiguration() {
        return new Configuration()
                .setProperty(AvailableSettings.DRIVER, YdbDriver.class.getName())
                .setProperty(AvailableSettings.DIALECT, YdbDialect.class.getName())
                .setProperty(AvailableSettings.HBM2DDL_AUTO, "create")
                .setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString())
                .setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString())
                .setProperty(AvailableSettings.HIGHLIGHT_SQL, Boolean.TRUE.toString());
    }

    public static String jdbcUrl(YdbHelperExtension ydb) {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }

    public static void inTransaction(Consumer<Session> work) {
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

    private TestUtils() {
    }
}
