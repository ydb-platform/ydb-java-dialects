package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

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
