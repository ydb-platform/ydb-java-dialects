package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
