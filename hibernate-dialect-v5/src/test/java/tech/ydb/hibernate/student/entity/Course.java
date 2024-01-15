package tech.ydb.hibernate.student.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.List;

/**
 * @author Kirill Kurdyukov
 */
@Getter
@Setter
@Entity
@Table(name = "Courses")
@NamedQuery(
        name = "Course.findCourses",
        query = "SELECT c FROM Course c " +
                "JOIN Plan p ON c.id = p.planId.courseId " +
                "JOIN Lecturer l ON p.planId.lecturerId = l.id " +
                "WHERE p.planId.groupId = :GroupId and l.id = :LecturerId"
)
public class Course {

    @Id
    @Column(name = "CourseId")
    private int id;

    @Column(name = "CourseName")
    private String name;

    @ManyToMany
    @JoinTable(
            name = "Marks",
            joinColumns = @JoinColumn(name = "CourseId"),
            inverseJoinColumns = @JoinColumn(name = "StudentId")
    )
    private List<Student> students;
}
