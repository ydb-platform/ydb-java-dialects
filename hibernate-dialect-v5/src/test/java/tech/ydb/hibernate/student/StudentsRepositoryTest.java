package tech.ydb.hibernate.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tech.ydb.hibernate.student.entity.Student;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kirill Kurdyukov
 */
public class StudentsRepositoryTest extends BaseTest {

    @ParameterizedTest
    @MethodSource(value = "studentStream")
    void studentByStudentIdTest(Student student) {
        inTransaction(
                entityManager -> {
                    Student findStudent = entityManager.find(Student.class, student.getStudentId());
                    assertEquals(student, findStudent);
                }
        );
    }

    @ParameterizedTest
    @MethodSource(value = "studentStream")
    void studentByStudentNameTest(Student student) {
        inTransaction(
                entityManager -> {
                    TypedQuery<Student> studentQuery = entityManager
                            .createQuery("FROM Student WHERE name = :v", Student.class);
                    studentQuery.setParameter("v", student.getName());

                    assertEquals(student.getName(), studentQuery.getResultList().get(0).getName());
                }
        );
    }

    @Test
    void studentsLimitAndOffsetTest() {
        inTransaction(
                entityManager -> {
                    TypedQuery<Student> studentQuery = entityManager
                            .createQuery("FROM Student", Student.class);
                    studentQuery.setMaxResults(2);
                    studentQuery.setFirstResult(2);

                    List<Student> students = studentQuery.getResultList();

                    assertEquals(2, students.size());
                    assertEquals("Петров П.П.", students.get(0).getName());
                    assertEquals("Сидров С.С.", students.get(1).getName());
                }
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void studentsOrderByStudentNameAndLimitTest() {
        inTransaction(
                entityManager -> {
                    Query studentQuery = entityManager
                            .createNativeQuery("SELECT * FROM Students ORDER BY name", Student.class)
                            .setMaxResults(2);

                    List<Student> students = studentQuery.getResultList();

                    assertEquals(2, students.size());
                    assertEquals("Безымянный Б.Б", students.get(0).getName());
                    assertEquals("Иванов И.И.", students.get(1).getName());
                }
        );
    }
}
