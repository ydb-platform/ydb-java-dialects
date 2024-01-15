package tech.ydb.hibernate.student.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Embeddable
public class PlanId implements Serializable {

    @Column(name = "GroupId")
    private int groupId;

    @Column(name = "CourseId")
    private int courseId;

    @Column(name = "LecturerId")
    private int lecturerId;
}
