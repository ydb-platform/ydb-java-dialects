package tech.ydb.hibernate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "Groups")
public class Group {

    @Id
    private int groupId;

    @Column(name = "name", unique = true)
    private String name;
}
