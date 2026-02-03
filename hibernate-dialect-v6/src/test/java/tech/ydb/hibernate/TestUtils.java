package tech.ydb.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import tech.ydb.hibernate.dialect.YdbDialect;
import tech.ydb.jdbc.YdbDriver;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.function.Consumer;

/**
 * @author Kirill Kurdyukov
 */
public abstract class TestUtils {

    public static SessionFactory SESSION_FACTORY;

    public static Configuration basedConfiguration() {
        return new Configuration()
                .setProperty(AvailableSettings.DRIVER, YdbDriver.class.getName())
                .setProperty(AvailableSettings.DIALECT, YdbDialect.class.getName())
                .setProperty(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create")
                .setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString())
                .setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString())
                .setProperty(AvailableSettings.HIGHLIGHT_SQL, Boolean.TRUE.toString());
    }

    public static String jdbcUrl(YdbHelperExtension ydb) {
        return "jdbc:ydb:" +
                (ydb.useTls() ? "grpcs://" : "grpc://") +
                ydb.endpoint() +
                ydb.database();
    }

    public static void inTransaction(Consumer<Session> work) {
        SESSION_FACTORY.inTransaction(work);
    }
}
