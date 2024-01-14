package tech.ydb.hibernate.student.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Kirill Kurdyukov
 */
@Data
public class PlanId implements Serializable {

    private Long GroupId;

    private Long CourseId;

    private Long LecturerId;
}
