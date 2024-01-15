package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.List;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Students")
public class Student {

    @Id
    @Column(name = "StudentId")
    private int id;

    @Column(name = "StudentName")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GroupId")
    private Group group;

    @ManyToMany(mappedBy = "students")
    private List<Course> courses;
}
