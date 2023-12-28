package tech.ydb.hibernate;

import org.hibernate.query.Query;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tech.ydb.hibernate.entity.Student;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kirill Kurdyukov
 */
public class StudentsRepositoryTest extends BaseTest {

    @ParameterizedTest
    @MethodSource(value = "studentStream")
    void studentByStudentIdTest(Student student) {
        SESSION_FACTORY.inTransaction(
                session -> {
                    Student findStudent = session.find(Student.class, student.getStudentId());
                    assertEquals(student, findStudent);
                }
        );
    }

    @ParameterizedTest
    @MethodSource(value = "studentStream")
    void studentByStudentNameTest(Student student) {
        SESSION_FACTORY.inTransaction(
                session -> {
                    Query<Student> studentQuery = session
                            .createQuery("FROM Student WHERE name = :v", Student.class);
                    studentQuery.setParameter("v", student.getName());

                    assertEquals(student.getName(), studentQuery.list().get(0).getName());
                }
        );
    }

    @Test
    void studentsOrderByStudentNameTest() {
        SESSION_FACTORY.inTransaction(
                session -> {
                    Query<Student> studentQuery = session
                            .createQuery("FROM Student ORDER BY name", Student.class);

                    List<Student> students = studentQuery.list();

                    assertEquals("Безымянный Б.Б", students.get(0).getName());
                    assertEquals("Иванов И.И.", students.get(1).getName());
                }
        );
    }

    @Test
    void studentsLimitAndOffsetTest() {
        SESSION_FACTORY.inTransaction(
                session -> {
                    Query<Student> studentQuery = session.createQuery("FROM Student", Student.class)
                            .setMaxResults(2)
                            .setFirstResult(2);

                    List<Student> students = studentQuery.list();

                    assertEquals("Безымянный Б.Б", students.get(0).getName());
                    assertEquals("Иванов И.И.", students.get(1).getName());
                }
        );
    }
}
