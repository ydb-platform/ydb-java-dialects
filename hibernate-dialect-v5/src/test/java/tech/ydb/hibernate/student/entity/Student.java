package tech.ydb.hibernate.student.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
