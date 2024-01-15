package tech.ydb.hibernate.student.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Marks")
public class Mark {

    @EmbeddedId
    private MarkId markId;

    @Column(name = "Mark")
    private int mark;
}
