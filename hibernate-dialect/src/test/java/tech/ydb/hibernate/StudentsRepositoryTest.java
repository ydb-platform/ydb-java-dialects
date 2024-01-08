package tech.ydb.hibernate;

import org.hibernate.query.Query;
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
    void studentsOrderByStudentNameAndLimitTest() {
        SESSION_FACTORY.inTransaction(
                session -> {
                    Query<Student> studentQuery = session
                            .createQuery("FROM Student ORDER BY name", Student.class)
                            .setMaxResults(2);

                    List<Student> students = studentQuery.list();

                    assertEquals(2, students.size());
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

                    assertEquals(2, students.size());
                    assertEquals("Петров П.П.", students.get(0).getName());
                    assertEquals("Сидоров С.С.", students.get(1).getName());
                }
        );
    }

    @Test
    void escapeLiteralTest() {
        SESSION_FACTORY.inTransaction(
                session -> {
                    Query<Student> studentQuery = session
                            .createQuery("FROM Student WHERE name LIKE '%Иван%' ESCAPE '\\'", Student.class);

                    Student student = studentQuery.getSingleResult();
                    assertEquals("Иванов И.И.", student.getName());
                }
        );

        SESSION_FACTORY.inTransaction(
                session -> {
                    session.persist(makeStudent(7, "Вопрос?", 1));
                    session.persist(makeStudent(8, "Подчеркивание_", 2));
                }
        );

        SESSION_FACTORY.inTransaction(
                session -> {
                    Query<Student> studentQuery = session
                            .createQuery("FROM Student WHERE name LIKE '%??%' ESCAPE '\\'", Student.class);

                    Student student = studentQuery.getSingleResult();
                    assertEquals("Вопрос?", student.getName());

                    session.remove(student);

                    studentQuery = session
                            .createQuery("FROM Student WHERE name LIKE '%?_%' ESCAPE '?'", Student.class);

                    student = studentQuery.getSingleResult();
                    assertEquals("Подчеркивание_", student.getName());

                    session.remove(student);
                }
        );
    }
}
