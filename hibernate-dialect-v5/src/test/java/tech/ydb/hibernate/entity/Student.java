package tech.ydb.hibernate.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Kirill Kurdyukov
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Table(name = "Students")
public class Student {

    @Id
    private int studentId;

    @Column(name = "name")
    private String name;

    @Column(name = "groupId")
    private int groupId;
}
