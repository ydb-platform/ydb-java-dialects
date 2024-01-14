package tech.ydb.hibernate.student.entity;

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
@Table(name = "Groups")
public class Group {

    @Id
    private int groupId;

    @Column(name = "name", unique = true)
    private String name;
}
