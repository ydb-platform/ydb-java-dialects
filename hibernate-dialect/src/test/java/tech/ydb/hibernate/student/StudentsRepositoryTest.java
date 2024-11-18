package tech.ydb.hibernate.student;

import java.util.List;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.query.Query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static tech.ydb.hibernate.TestUtils.SESSION_FACTORY;
import static tech.ydb.hibernate.TestUtils.basedConfiguration;
import static tech.ydb.hibernate.TestUtils.inTransaction;
import static tech.ydb.hibernate.TestUtils.jdbcUrl;
import tech.ydb.hibernate.student.entity.Course;
import tech.ydb.hibernate.student.entity.Group;
import tech.ydb.hibernate.student.entity.Lecturer;
import tech.ydb.hibernate.student.entity.Mark;
import tech.ydb.hibernate.student.entity.Plan;
import tech.ydb.hibernate.student.entity.Student;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
public class StudentsRepositoryTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void beforeAll() {
        Configuration configuration = basedConfiguration()
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, "import-university.sql");

        for (Class<?> entity : new Class<?>[]
                {Course.class, Group.class, Lecturer.class, Mark.class, Plan.class, Student.class}
        ) {
            configuration.addAnnotatedClass(entity);
        }

        SESSION_FACTORY = configuration.buildSessionFactory();

        SESSION_FACTORY.getCache().evictAllRegions();
    }

    @Test
    void studentByStudentIdTest_Eager_ManyToOne() {
        inTransaction(
                session -> {
                    Student student = session.find(Student.class, 4);

                    assertEquals("Сидоров С.С.", student.getName());
                }
        );

        // Forced lazy join
        inTransaction(
                session -> {
                    Student student = session.createQuery(
                            "SELECT s FROM Student s JOIN FETCH s.group WHERE s.id = 4",
                            Student.class
                    ).getSingleResult();

                    assertEquals("Сидоров С.С.", student.getName());
                    assertEquals("M3439", student.getGroup().getName());
                    assertEquals(2, student.getGroup().getId());
                }
        );
    }

    @Test
    void studentByStudentNameTest_Lazy_ManyToOne() {
        inTransaction(
                session -> {
                    Query<Student> studentQuery = session
                            .createQuery("FROM Student WHERE name = :v", Student.class);
                    studentQuery.setParameter("v", "Сидоров С.С.");

                    Student student = studentQuery.getSingleResult();

                    assertEquals(4, student.getId());

                    // Lazy select
                    assertEquals("M3439", student.getGroup().getName());
                    assertEquals(2, student.getGroup().getId());
                }
        );
    }

    @Test
    void studentsOrderByStudentNameAndLimitTest() {
        inTransaction(
                session -> {
                    Query<Student> studentQuery = session
                            .createQuery("FROM Student ORDER BY name", Student.class)
                            .setMaxResults(2);

                    List<Student> students = studentQuery.getResultList();

                    assertEquals(2, students.size());
                    assertEquals("Безымянный Б.Б", students.get(0).getName());
                    assertEquals("Иванов И.И.", students.get(1).getName());
                }
        );
    }

    @Test
    void studentsLimitAndOffsetTest() {
        inTransaction(
                session -> {
                    Query<Student> studentQuery = session.createQuery("FROM Student", Student.class)
                            .setMaxResults(2)
                            .setFirstResult(2);

                    List<Student> students = studentQuery.getResultList();

                    assertEquals(2, students.size());
                    assertEquals("Петров П.П.", students.get(0).getName());
                    assertEquals("Сидоров С.С.", students.get(1).getName());
                }
        );
    }

    @Test
    void studentsAndCourses_Lazy_ManyToManyTest() {
        inTransaction(
                session -> {
                    List<Student> students = session.find(Course.class, 1).getStudents();

                    assertEquals(3, students.size());
                }
        );

        inTransaction(
                session -> {
                    List<Course> courses = session.find(Student.class, 1).getCourses();

                    assertEquals(1, courses.size());
                    assertEquals("Базы данных", courses.get(0).getName());
                }
        );
    }

    @Test
    void studentsByGroupName_Lazy_OneToManyTest() {
        inTransaction(
                session -> {
                    List<Student> students = session
                            .createQuery("FROM Group g WHERE g.name = 'M3439'", Group.class)
                            .getSingleResult().getStudents();

                    assertEquals(2, students.size());

                    assertEquals("Петров П.П.", students.get(0).getName());
                    assertEquals("Сидоров С.С.", students.get(1).getName());
                }
        );
    }

    @Test
    void groupByGroupName_ViewIndex() {
        inTransaction(
                session -> {
                    Group group = session
                            .createQuery("FROM Group g WHERE g.name = 'M3439'", Group.class)
                            .addQueryHint("use_index:group_name_index") // Hibernate
                            .getSingleResult();

                    assertEquals("M3439", group.getName());
                }
        );

        inTransaction(
                session -> {
                    Group group = session
                            .createQuery("FROM Group g WHERE g.name = 'M3439'", Group.class)
                            .setHint(HibernateHints.HINT_COMMENT, "use_index:group_name_index") // JPA
                            .getSingleResult();

                    assertEquals("M3439", group.getName());
                }
        );
    }

    @Test
    void studentsByGroupName_Eager_OneToManyTest() {
        inTransaction(
                session -> {
                    List<Student> students = session
                            .createQuery("FROM Group g JOIN FETCH g.students WHERE g.name = 'M3439'", Group.class)
                            .getSingleResult().getStudents();

                    assertEquals(2, students.size());

                    assertEquals("Петров П.П.", students.get(0).getName());
                    assertEquals("Сидоров С.С.", students.get(1).getName());
                }
        );
    }

    @Test
    void studentByGroupNameUsingPragmaHintTest() {
        inTransaction(
                session -> {
                    List<Student> students = session
                            .createQuery("FROM Group g JOIN FETCH g.students WHERE g.name = 'M3439'", Group.class)
                            .addQueryHint("pragma:ydb.HashJoinMode='grace'")
                            .addQueryHint("pragma:Warning(\"disable\", \"1101\")")
                            .getSingleResult().getStudents();

                    assertEquals(2, students.size());

                    assertEquals("Петров П.П.", students.get(0).getName());
                    assertEquals("Сидоров С.С.", students.get(1).getName());
                }
        );
    }

    @Test
    void groupsByLecturerIdAndCourseId_Eager_ManyToManyTest() {
        inTransaction(
                session -> {
                    List<Group> groups = session
                            .createNamedQuery("Group.findGroups", Group.class)
                            .setParameter("LecturerId", 1)
                            .setParameter("CourseId", 1)
                            .getResultList();

                    assertEquals(1, groups.size());
                    assertEquals("M3439", groups.get(0).getName());
                }
        );
    }

    @Test
    void coursesByLecturerIdAndGroupId_Eager_ManyToManyTest() {
        inTransaction(
                session -> {
                    List<Course> courses = session
                            .createNamedQuery("Course.findCourses", Course.class)
                            .setParameter("GroupId", 1)
                            .setParameter("LecturerId", 1)
                            .getResultList();

                    assertEquals(1, courses.size());
                    assertEquals("Технологии Java", courses.get(0).getName());
                }
        );
    }

    @Test
    void sumMarkByStudentIdTest() {
        inTransaction(
                session -> {
                    long sumMark = session.createQuery(
                            "select sum(m.mark) from Mark m where m.id.studentId = 2",
                            Long.class
                    ).getSingleResult();

                    assertEquals(7, sumMark);
                }
        );
    }

    @Test
    void sumAvgByStudentIdTest() {
        inTransaction(
                session -> {
                    double sumMark = session.createQuery(
                            "select avg(cast(m.mark as float)) from Mark m where m.id.studentId = 2",
                            Double.class
                    ).getSingleResult();

                    assertEquals(3.5, sumMark);
                }
        );
    }

    @Test
    void useScanQueryHintTest() {
        /*
        scan select
            c1_0.CourseId,
            c1_0.CourseName
        from
            Courses c1_0
        order by
            c1_0.CourseId
         */
        inTransaction(
                session -> {
                    var courses = session.createQuery("FROM Course c ORDER BY c.id", Course.class)
                            .addQueryHint("use_scan")
                            .getResultList();

                    checkCourses(courses);
                }
        );

        /*
        scan select
            c1_0.CourseId,
            c1_0.CourseName
        from
            Courses c1_0
        order by
            c1_0.CourseId
         */
        inTransaction(
                session -> {
                    var courses = session.createQuery("FROM Course c ORDER BY c.id", Course.class)
                            .setHint(HibernateHints.HINT_COMMENT, "use_scan")
                            .getResultList();

                    checkCourses(courses);
                }
        );
    }

    private static void checkCourses(List<Course> courses) {
        assertEquals(6, courses.size());
        assertEquals("Базы данных", courses.get(0).getName());
        assertEquals("Управление проектами", courses.get(1).getName());
        assertEquals("ППО", courses.get(2).getName());
        assertEquals("Теория информации", courses.get(3).getName());
        assertEquals("Математический анализ", courses.get(4).getName());
        assertEquals("Технологии Java", courses.get(5).getName());
    }

    @Test
    void useAllHintsTogetherTest() {
        /*
        PRAGMA Warning("disable", "1101");
        scan select
            g1_0.GroupId,
            g1_0.GroupName
        from
            Groups view group_name_index g1_0
        where
            g1_0.GroupName='M3439'
         */
        inTransaction(
                session -> {
                    Group group = session
                            .createQuery("FROM Group g WHERE g.name = 'M3439'", Group.class)
                            .addQueryHint("use_index:group_name_index") // Hibernate
                            .addQueryHint("use_scan")
                            .addQueryHint("pragma:Warning(\"disable\", \"1101\")")
                            .getSingleResult();

                    assertEquals("M3439", group.getName());
                }
        );


        /*
        PRAGMA Warning("disable", "1101");
        scan select
            g1_0.GroupId,
            g1_0.GroupName
        from
            Groups view group_name_index g1_0
        where
            g1_0.GroupName='M3439'
         */
        inTransaction(
                session -> {
                    Group group = session
                            .createQuery("FROM Group g WHERE g.name = 'M3439'", Group.class)
                            .setHint(HibernateHints.HINT_COMMENT,
                                    "use_index:group_name_index;use_scan;pragma:Warning(\"disable\", \"1101\")")
                            .getSingleResult();

                    assertEquals("M3439", group.getName());
                }
        );
    }
}
