package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Students")
public class Student {

    @Id
    @Column(name = "StudentId")
    private Long id;

    @Column(name = "StudentName")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Group group;
}
