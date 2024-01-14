package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Courses")
public class Course {

    @Id
    @Column(name = "CourseId")
    private Long id;

    @Column(name = "CourseName")
    private String name;
}
