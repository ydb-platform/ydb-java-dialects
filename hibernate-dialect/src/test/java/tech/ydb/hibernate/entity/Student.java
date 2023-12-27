package tech.ydb.hibernate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
