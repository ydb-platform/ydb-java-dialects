package tech.ydb.hibernate.student;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.hibernate.dialect.YdbDialect;
import tech.ydb.hibernate.student.entity.Group;
import tech.ydb.hibernate.student.entity.Student;
import tech.ydb.jdbc.YdbDriver;
import tech.ydb.test.junit5.YdbHelperExtension;

import javax.persistence.EntityManager;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Kirill Kurdyukov
 */
public abstract class BaseTest {

    @RegisterExtension
    public static final YdbHelperExtension YDB_HELPER_EXTENSION = new YdbHelperExtension();

    protected static SessionFactory SESSION_FACTORY;

    @BeforeAll
    static void beforeAll() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(YDB_HELPER_EXTENSION.useTls() ? "grpcs://" : "grpc://")
                .append(YDB_HELPER_EXTENSION.endpoint())
                .append(YDB_HELPER_EXTENSION.database());

        if (YDB_HELPER_EXTENSION.authToken() != null) {
            jdbc.append("?").append("token=").append(YDB_HELPER_EXTENSION.authToken());
        }

        Properties properties = new Properties();
        properties.put(Environment.DRIVER, YdbDriver.class.getName());
        properties.put(Environment.URL, jdbc.toString());
        properties.put(Environment.DIALECT, YdbDialect.class.getName());
        properties.put(Environment.HBM2DDL_AUTO, "create");
        properties.put(Environment.SHOW_SQL, true);

        SESSION_FACTORY = new Configuration().addProperties(properties)
                .addAnnotatedClass(Student.class)
                .addAnnotatedClass(Group.class)
                .buildSessionFactory();

        inTransaction(entityManager -> {
            entityManager.persist(makeGroup(1, "M3435"));
            entityManager.persist(makeGroup(2, "M3439"));
            entityManager.persist(makeGroup(3, "M3238"));
            entityManager.persist(makeGroup(4, "M3239"));

            studentStream().forEach(entityManager::persist);
        });
    }

    protected static Group makeGroup(int groupId, String name) {
        Group group = new Group();

        group.setGroupId(groupId);
        group.setName(name);
        return group;
    }

    protected static Student makeStudent(int studentId, String name, int groupId) {
        Student student = new Student();

        student.setStudentId(studentId);
        student.setName(name);
        student.setGroupId(groupId);
        return student;
    }

    protected static Stream<Student> studentStream() {
        return Stream.of(
                makeStudent(1, "Иванов И.И.", 1),
                makeStudent(2, "Петров П.П.", 1),
                makeStudent(3, "Петров П.П.", 2),
                makeStudent(4, "Сидров С.С.", 2),
                makeStudent(5, "Неизвестный Н.Н.", 3),
                makeStudent(6, "Безымянный Б.Б", 4)
        );
    }

    protected static void inTransaction(Consumer<EntityManager> testCase) {
        EntityManager entityManager = SESSION_FACTORY.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            testCase.accept(entityManager);
        } finally {
            entityManager.getTransaction().commit();
        }
    }
}
