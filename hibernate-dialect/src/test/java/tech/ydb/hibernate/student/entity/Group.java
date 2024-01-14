package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Set;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Groups")
public class Group {

    @Id
    @Column(name = "GroupId")
    private Long id;

    @Column(name = "GroupName")
    private String name;

    @OneToMany(mappedBy = "group")
    private Set<Student> students;
}
