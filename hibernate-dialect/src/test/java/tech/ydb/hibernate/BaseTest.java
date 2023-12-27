package tech.ydb.hibernate;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.extension.ExtendWith;
import tech.ydb.hibernate.dialect.YdbDialect;
import tech.ydb.hibernate.entity.Group;
import tech.ydb.hibernate.entity.Student;
import tech.ydb.jdbc.YdbDriver;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;
import tech.ydb.test.junit5.GrpcTransportExtension;

import java.util.Properties;
import java.util.stream.Stream;

/**
 * @author Kirill Kurdyukov
 */
@ExtendWith(GrpcTransportExtension.class)
public abstract class BaseTest {

    protected static final SessionFactory SESSION_FACTORY;

    static {
        YdbHelper ydb = YdbHelperFactory.getInstance().createHelper();

        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        Properties properties = new Properties();
        properties.put(Environment.DRIVER, YdbDriver.class.getName());
        properties.put(Environment.URL, jdbc.toString());
        properties.put(Environment.DIALECT, YdbDialect.class.getName());
        properties.put(Environment.HBM2DDL_AUTO, "update");
        properties.put(Environment.SHOW_SQL, true);

        SESSION_FACTORY = new Configuration().addProperties(properties)
                .addAnnotatedClass(Student.class)
                .addAnnotatedClass(Group.class)
                .buildSessionFactory();

        EntityManager entityManager = SESSION_FACTORY.createEntityManager();
        entityManager.getTransaction().begin();

        entityManager.persist(makeGroup(1, "M3435"));
        entityManager.persist(makeGroup(2, "M3439"));
        entityManager.persist(makeGroup(3, "M3238"));
        entityManager.persist(makeGroup(4, "M3239"));

        studentStream().forEach(entityManager::persist);

        entityManager.flush();
        entityManager.getTransaction().commit();
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
}
