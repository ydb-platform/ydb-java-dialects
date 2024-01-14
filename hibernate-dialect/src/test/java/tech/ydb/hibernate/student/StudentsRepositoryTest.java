package tech.ydb.hibernate.student;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.ydb.hibernate.BaseTest;
import tech.ydb.hibernate.dialect.YdbDialect;
import tech.ydb.hibernate.student.entity.Course;
import tech.ydb.hibernate.student.entity.Group;
import tech.ydb.hibernate.student.entity.Lecturer;
import tech.ydb.hibernate.student.entity.Mark;
import tech.ydb.hibernate.student.entity.Plan;
import tech.ydb.hibernate.student.entity.Student;
import tech.ydb.jdbc.YdbDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kirill Kurdyukov
 */
public class StudentsRepositoryTest extends BaseTest {

    @BeforeAll
    static void beforeAll() {
        Configuration configuration = new Configuration()
                .setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, YdbDriver.class.getName())
                .setProperty(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl())
                .setProperty(AvailableSettings.DIALECT, YdbDialect.class.getName())
                .setProperty(AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE, "schema.sql")
                .setProperty(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, "import.sql")
                .setProperty(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "validate");

        for (Class<?> entity : new Class<?>[]
                {Course.class, Group.class, Lecturer.class, Mark.class, Plan.class, Student.class}
        ) {
            configuration.addAnnotatedClass(entity);
        }

        SESSION_FACTORY = configuration.buildSessionFactory();
    }


    @Test
    void studentByStudentIdTest() {
        inTransaction(
                session -> {
                    Student student = session.find(Student.class, 4);

                    assertEquals("Сидоров С.C.", student.getName());
                }
        );
    }

//    @Test
//    void studentByStudentNameTest(Student student) {
//        inTransaction(
//                session -> {
//                    TypedQuery<Student> studentQuery = session
//                            .createQuery("FROM Student WHERE name = :v", Student.class);
//                    studentQuery.setParameter("v", student.getName());
//
//                    assertEquals(student.getName(), studentQuery.list().get(0).getName());
//                }
//        );
//    }
//
//    @Test
//    void studentsOrderByStudentNameAndLimitTest() {
//        inTransaction(
//                session -> {
//                    TypedQuery<Student> studentQuery = session
//                            .createQuery("FROM Student ORDER BY name", Student.class)
//                            .setMaxResults(2);
//
//                    List<Student> students = studentQuery.getResultList();
//
//                    assertEquals(2, students.size());
//                    assertEquals("Безымянный Б.Б", students.get(0).getName());
//                    assertEquals("Иванов И.И.", students.get(1).getName());
//                }
//        );
//    }
//
//    @Test
//    void studentsLimitAndOffsetTest() {
//        inTransaction(
//                session -> {
//                    TypedQuery<Student> studentQuery = session.createQuery("FROM Student", Student.class)
//                            .setMaxResults(2)
//                            .setFirstResult(2);
//
//                    List<Student> students = studentQuery.getResultList();
//
//                    assertEquals(2, students.size());
//                    assertEquals("Петров П.П.", students.get(0).getName());
//                    assertEquals("Сидоров С.С.", students.get(1).getName());
//                }
//        );
//    }
}
