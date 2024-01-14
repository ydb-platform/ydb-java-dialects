package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Marks")
@IdClass(MarkId.class)
public class Mark {

    @Id
    @ManyToOne
    @JoinColumn(name = "StudentId", referencedColumnName = "StudentId")
    private Student student;

    @Id
    @ManyToOne
    @JoinColumn(name = "CourseId", referencedColumnName = "CourseId")
    private Course course;

    @Column(name = "Mark")
    private int mark;
}
